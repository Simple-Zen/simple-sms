package io.simplezen.simple_sms.inbound_messaging

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.net.toUri
import io.simplezen.simple_sms.queries.Query
import io.simplezen.simple_sms.QueryObj
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.get

// Inbound MMS Messages
class InboundMmsHandler() : BroadcastReceiver() {
    companion object {
        const val EXTRA_SUBSCRIPTION = "subscription"
        const val EXTRA_DATA = "data"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Log the incoming Intent
        Log.d("IncomingMmsHandler", " <<< Received MMS - $intent")
        Log.d("IncomingMmsHandler", " <<< Received MMS - ${intent.extras!!.keySet()}")
        Log.d("IncomingMmsHandler", " <<< Intent data: ${intent.data}")
        Log.d("IncomingMmsHandler", " <<< Intent action: ${intent.dataString}")

        // Check if the action string matches the expected value
        if (intent.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION
            && intent.action != Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION
        ) {
            Log.w("IncomingMmsHandler", " <<< Received unexpected action: ${intent.action}")
            return
        }

        // Check if the intent is for an MMS message
//        if (intent.type != ContentType.MMS_MESSAGE) {
//            Log.w("IncomingMmsHandler", " <<< Received non-MMS message")
//            return
//        }
        val bundle : Bundle = intent.extras!!

        val subId: Int = intent.getIntExtra(
            EXTRA_SUBSCRIPTION,
            -1
        )

        val data: ByteArray? = intent.getByteArrayExtra(EXTRA_DATA)

        val action = ReceiveMmsMessageAction(subId, data)
        action.start()

        // Get the TelephonyManager instance from context
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        // Check if the device supports SMS
        val smsCapable: Boolean = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_MESSAGING)
        if (!smsCapable) {
            Log.w("IncomingMmsHandler", " <<< Device does not support SMS")
            return
        }

        // Verify we're set to the default SMS app
        val isDefaultSmsApp: Boolean = context.packageName == Telephony.Sms.getDefaultSmsPackage(context)
        if (!isDefaultSmsApp) {
            Log.w("IncomingMmsHandler", " <<< Device is not set to the default SMS app")
            return
        }

        // TODO: Verify incoming MMS messages are written to the content://mms provider

        // TODO: SubId vs TransactionId?
        // Extract transaction ID which is needed for MMS processing
        val transactionId = bundle.getInt("transactionId")
        Log.d("IncomingMmsHandler", " <<< Transaction ID: $transactionId")

        setResult(Activity.RESULT_OK, null, null)
        resultCode = Activity.RESULT_OK
        val pendingResult = goAsync()

        // Start a coroutine to handle MMS processing
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val contentResolver = context.contentResolver
                val mmsInboxUri = "content://mms".toUri()



                val uri = contentResolver.insert(
                    mmsInboxUri,
                    ContentValues().apply {
                        put(Telephony.Mms.Inbox.SUBSCRIPTION_ID, subId)
                        put(Telephony.Mms.Inbox.TRANSACTION_ID, transactionId)
                    },
                    bundle

                )


                Log.d("IncomingMmsHandler", " <<< Using MMS URI: $uri")
                val messageId =  ContentUris.parseId(uri!!)

                // Ensure MMS message is fully downloaded
//                val success = ensureMmsDownloaded(context, messageId)
//                if (!success) {
//                    Log.d("IncomingMmsHandler", " <<< Waiting for MMS to complete downloading...")
//                    delay(2000) // Wait a bit more
//                }

                // Query for message data
                val query = QueryObj(
                    contentUri = uri.toString(),
                    projection = emptyList(),
                    sortOrder = null
                )
                val queryResult = Query(context).query(query)

                if (queryResult.isEmpty()) {
                    throw Exception("No MMS data found for ID: $messageId after waiting")
                }

                val msgMap = queryResult.first() as Map<*, *>
                Log.d("IncomingMmsHandler", " <<< MMS data: ${msgMap.keys.joinToString()}")

                // Process message parts to get attachments
                val messageData = getMmsPartData(context, messageId)
                if (messageData.isNotEmpty()) {
                    val attachments = mutableListOf<Map<String, Any?>>()
                    var bodyText = ""
                    var hasImageAttachment = false

                    // First pass: identify if we have any image attachments
                    for (part in messageData) {
                        val contentType = part["ct"]?.toString() ?: "application/octet-stream"
                        if (contentType.startsWith("image/")) {
                            hasImageAttachment = true
                            break
                        }
                    }

                    // Second pass: process parts
                    for (part in messageData) {
                        val partId = part["_id"]?.toString() ?: ""
                        val contentType = part["ct"]?.toString() ?: "application/octet-stream"

                        Log.d(
                            "IncomingMmsHandler",
                            " <<< Processing MMS part: id=$partId, type=$contentType"
                        )

                        // Extract text parts for the body (unless we have images - then we can skip text)
                        if (contentType == "text/plain" && (!hasImageAttachment || bodyText.isEmpty())) {
                            val text = part["text"]?.toString() ?: ""
                            if (text.isNotEmpty()) {
                                bodyText += " $text"
                                Log.d("IncomingMmsHandler", " <<< Found text part: '$text'")
                            }
                        } else if (partId.isNotEmpty()) {
                            // Add non-text parts as attachments
                            val attachmentMap = mapOf(
                                "path" to "content://mms/part/$partId",
                                "mimeType" to contentType
                            )
                            attachments.add(attachmentMap)
                            Log.d("IncomingMmsHandler", " <<< Added attachment: $attachmentMap")
                        }
                    }

                    // Create a properly formatted MMS message for Flutter
                    // Using the exact format Flutter expects based on AndroidMms.fromJson
                    val flutterMmsMessage = mutableMapOf<String, Any?>()

                    // Required fields for AndroidMms.fromJson
                    flutterMmsMessage["_id"] = messageId
                    flutterMmsMessage["thread_id"] = msgMap["thread_id"] ?: ""
                    flutterMmsMessage["type"] = "mms"  // This identifies it as MMS for Flutter
                    flutterMmsMessage["message_type"] =
                        msgMap["m_type"] ?: "132"  // 132 is MMS received
                    flutterMmsMessage["msg_box"] = msgMap["msg_box"] ?: "1"        // 1 is inbox
                    flutterMmsMessage["read"] = msgMap["read"] ?: "0"
                    flutterMmsMessage["date"] = msgMap["date"] ?: "0"
                    flutterMmsMessage["date_sent"] = msgMap["date_sent"] ?: "0"
                    flutterMmsMessage["sim_slot"] = msgMap["sim_slot"] ?: "0"

                    // Add the body text if available, or empty if not
                    flutterMmsMessage["body"] = if (bodyText.isNotEmpty()) bodyText.trim() else ""

                    // If using auto-generated address (e.g. from Google Messages), try to extract a better address
                    val address = msgMap["from_address"]?.toString() ?: ""
                    flutterMmsMessage["address"] =
                        if (address.isNotEmpty()) address else msgMap["creator"]?.toString() ?: ""

                    // Add attachments as an array that Flutter can process
                    if (attachments.isNotEmpty()) {
                        flutterMmsMessage["attachments"] = attachments
                        Log.d(
                            "IncomingMmsHandler",
                            " <<< Added ${attachments.size} attachments to message"
                        )
                    }

                    // Additional debug logging
                    Log.d(
                        "IncomingMmsHandler",
                        " <<< Sending MMS message to Flutter: $flutterMmsMessage"
                    )
                    Log.d(
                        "IncomingMmsHandler",
                        " <<< Message has body: ${flutterMmsMessage["body"]}"
                    )
                    Log.d("IncomingMmsHandler", " <<< Message has attachments: ${attachments.size}")

                    // Switch to the main thread for Flutter communication
                    withContext(Dispatchers.Main) {
                        InboundMessaging(context).receiveInboundMessage(flutterMmsMessage)
                    }
                } else {
                    // Convert original map to expected format
                    val convertedMap = mutableMapOf<String, Any?>()
                    msgMap.forEach { (key, value) ->
                        convertedMap[key.toString()] = value
                    }

                    // Add message type to identify this as MMS for Flutter
                    convertedMap["type"] = "mms"
                    convertedMap["message_type"] = msgMap["m_type"] ?: "132"  // 132 is MMS received

                    // Add address field if missing
                    if (!convertedMap.containsKey("address")) {
                        convertedMap["address"] = convertedMap["from_address"] ?: ""
                    }

                    // Switch to the main thread for Flutter communication
                    withContext(Dispatchers.Main) {
                        InboundMessaging(context).receiveInboundMessage(convertedMap)
                        pendingResult.resultCode = Activity.RESULT_OK
                        pendingResult.finish()
                    }
                    Log.d("IncomingMmsHandler", " <<< No MMS parts found, sending original message")
                }
                Log.d("IncomingMmsHandler", " <<< No MMS parts found, sending original messagez")
            } catch (e: Exception) {
                Log.e("IncomingMmsHandler", " <<<<< Error: $e")
                Log.e("IncomingMmsHandler", " <<<<< Error: ${e.stackTraceToString()}")
            }
            Log.d("IncomingMmsHandler", " <<< No MMS parts found, sending original messageasdf")
        }
        Log.d("IncomingMmsHandler", " <<< No MMS parts found, sending original messagesrgbsr ")
//        pendingResult.resultCode = Activity.RESULT_OK
//        pendingResult.finish()
    }

    // Find MMS by transaction ID
    private fun findMmsByTransactionId(context: Context, transactionId: String?): Int? {
        if (transactionId.isNullOrEmpty()) return null

        Log.d("InboundMmsHandler", "Looking for MMS with transaction ID: $transactionId")

        Log.d("InboundMmsHandler", " <<< sleeping for 2 seconds")
        Thread.sleep(2000)
        Log.d("InboundMmsHandler", " <<< waking up")


        try {
            val contentResolver = context.contentResolver
            val mmsInboxUri = "content://mms".toUri()

            // First try to find by transaction ID directly
            val transactionCursor = contentResolver.query(
                mmsInboxUri,
                arrayOf("_id"),
                "tr_id = ?",
                arrayOf(transactionId),
                "date DESC"
            )

            transactionCursor?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getInt(0)
                    Log.d("InboundMmsHandler", "Found MMS by transaction ID: $id")
                    return id
                }
            }

            // If not found by transaction ID, just get most recent MMS
            var id: Int? = null
            val recentCursor = contentResolver.query(
                mmsInboxUri,
                arrayOf("_id"),
                "msg_box=1", // Only inbox items
                null,
                "date DESC LIMIT 1" // Get most recent
            )

            recentCursor?.use { cursor ->
                if (cursor.moveToFirst()) {
                    id = cursor.getInt(0)
                    Log.d("InboundMmsHandler", "Found most recent MMS: $id")
                }
            }

            return id

        } catch (e: Exception) {
            Log.e("InboundMmsHandler", "Error finding MMS by transaction ID: $e")
            return null
        }
    }

    // Ensure MMS is downloaded
    private fun ensureMmsDownloaded(context: Context, messageId: Int): Boolean {
        try {
            val uri = "content://mms/$messageId".toUri()
            val values = ContentValues().apply {
                put(Telephony.Mms.READ, 1)
            }
            context.contentResolver.update(uri, values, null, null)
            return true
        } catch (e: Exception) {
            Log.e("IncomingMmsHandler", " <<< Error marking MMS as read: $e")
            return false
        }
    }


    // Helper method to get MMS part data (attachments)
    private fun getMmsPartData(context: Context, mmsId: Long): List<Map<*, *>> {
        try {
            // First try using the Query class
            val query = QueryObj(
                contentUri = "content://mms/part",
                selection = "mid = ?",
                selectionArgs = listOf(mmsId.toString()),
                sortOrder = "_id ASC"
            )

            try {

                val contentResolver = context.contentResolver
                val mmsUri = "content://mms/part".toUri()
                val mmsCursor = contentResolver.query(
                    mmsUri,
                    null,  // Get all columns
                    "mid = $mmsId",
                    null,
                    "_id ASC"
                )
                mmsCursor?.use { cursor ->
                    val columnNames = cursor.columnNames.toList()
                    val parts = mutableListOf<Map<String, Any?>>()
                    while (cursor.moveToNext()) {
                        val part = mutableMapOf<String, Any?>()
                        for (i in 0 until cursor.columnCount) {
                            val columnName = columnNames[i]
                            when (cursor.getType(i)) {
                                Cursor.FIELD_TYPE_NULL -> part[columnName] = null
                                Cursor.FIELD_TYPE_INTEGER -> part[columnName] = cursor.getLong(i)
                                Cursor.FIELD_TYPE_FLOAT -> part[columnName] = cursor.getFloat(i)
                                Cursor.FIELD_TYPE_STRING -> part[columnName] = cursor.getString(i)
                                Cursor.FIELD_TYPE_BLOB -> part[columnName] = cursor.getBlob(i)
                            }
                        }
                        parts.add(part)
                    }
                    Log.d("IncomingMmsHandler", " <<< Found ${parts.size} MMS parts via ContentResolver")
                    return parts

                }

                val result = Query(context).query(query)
                if (result.isNotEmpty()) {
                    Log.d("IncomingMmsHandler", " <<< Found ${result.size} MMS parts via Query")
                    return result
                }
            } catch (e: Exception) {
                Log.e("IncomingMmsHandler", " <<< Error getting MMS parts via Query: $e")
            }

            // Fallback to direct ContentResolver query
            Log.d(
                "IncomingMmsHandler",
                " <<< Falling back to direct ContentResolver query for MMS parts"
            )
            val cursor = context.contentResolver.query(
                "content://mms/part".toUri(),
                null,  // Get all columns
                "mid = ?",
                arrayOf(mmsId.toString()),
                "_id ASC"
            )

            val parts = mutableListOf<Map<String, Any?>>()
            cursor?.use {
                val columnNames = it.columnNames.toList()
                while (it.moveToNext()) {
                    val part = mutableMapOf<String, Any?>()
                    for (i in 0 until it.columnCount) {
                        val columnName = columnNames[i]
                        when (it.getType(i)) {
                            Cursor.FIELD_TYPE_NULL -> part[columnName] = null
                            Cursor.FIELD_TYPE_INTEGER -> part[columnName] = it.getLong(i)
                            Cursor.FIELD_TYPE_FLOAT -> part[columnName] = it.getFloat(i)
                            Cursor.FIELD_TYPE_STRING -> part[columnName] = it.getString(i)
                            Cursor.FIELD_TYPE_BLOB -> part[columnName] = it.getBlob(i)
                        }
                    }
                    parts.add(part)
                }
            }

            Log.d("IncomingMmsHandler", " <<< Found ${parts.size} MMS parts via ContentResolver")
            return parts
        } catch (e: Exception) {
            Log.e("IncomingMmsHandler", " <<< Error getting MMS parts: $e")
            return emptyList()
        }
    }

    // Find recently received MMS messages (better than just latest)
    private fun findRecentMms(context: Context, startTime: Long): Int? {
        try {
            // Calculate a time window - look at messages no more than 30 seconds before
            // this intent was received, to avoid grabbing old messages
            val timeWindow = (startTime / 1000) - 30 // Convert to seconds and look back 30s max

            Log.d("IncomingMmsHandler", " <<< Looking for MMS messages since timestamp: $timeWindow")

            // On Android 14+, the provider may have additional delays
            // We need to be less restrictive in our time window
            val timeFilter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // More lenient on Android 14+
                "date >= ?"
            } else {
                "date >= ?"
            }

            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                "content://mms".toUri(),
                arrayOf("_id", "msg_box", "date"),
                timeFilter,
                arrayOf(timeWindow.toString()),
                "date DESC"
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    // Prioritize messages in the inbox (msg_box = 1)
                    val idIndex = it.getColumnIndex("_id")
                    val boxIndex = it.getColumnIndex("msg_box")

                    // First pass to find inbox messages
                    if (boxIndex >= 0 && idIndex >= 0) {
                        do {
                            val msgBox = it.getInt(boxIndex)
                            if (msgBox == 1) { // 1 is inbox
                                val id = it.getInt(idIndex)
                                Log.d("IncomingMmsHandler", " <<< Selected recent inbox MMS: $id")
                                return id
                            }
                        } while (it.moveToNext())

                        // Reset cursor if we didn't find an inbox message
                        it.moveToFirst()
                    }

                    // Just take the first one if no inbox messages found
                    if (idIndex >= 0) {
                        val id = it.getInt(idIndex)
                        Log.d("IncomingMmsHandler", " <<< Selected recent MMS: $id")
                        return id
                    }
                }
            }

            return null
        } catch (e: Exception) {
            Log.e("IncomingMmsHandler", " <<< Error finding recent MMS: $e")
            return null
        }
    }
}