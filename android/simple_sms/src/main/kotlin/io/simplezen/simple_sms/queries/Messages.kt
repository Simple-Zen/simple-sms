package io.simplezen.simple_sms.queries

//import android.provider.Telephony
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony.Mms
import androidx.core.net.toUri
import io.flutter.Log

//
//enum class MessagesFilter {
//    AllMms,
//    AllSms,
//    Draft,
//    Threads,
//    PhoneNumber,
//    Locked,
//    Undelivered,
//    Search;
//
//
//    fun toUri(searchParams: List<String>? = null): Uri {
//        Log.d("MessageQuery", "FilterType: $this")
//        Log.d("MessageQuery", "FilterType: ${Telephony.MmsSms.CONTENT_URI}")
//        return when {
//            this == AllMms -> Mms.CONTENT_URI
//            this == AllSms -> Telephony.Sms.CONTENT_URI
//            this == Draft -> Telephony.MmsSms.CONTENT_DRAFT_URI
//            this == Threads -> Telephony.MmsSms.CONTENT_CONVERSATIONS_URI // Threads instead?
//            this == Locked -> Telephony.MmsSms.CONTENT_LOCKED_URI
//            this == Undelivered -> Telephony.MmsSms.CONTENT_UNDELIVERED_URI
//            this == PhoneNumber ->
//                Uri.parse(
//                    "${Telephony.MmsSms.CONTENT_FILTER_BYPHONE_URI}/$searchParams"
//                )
//            this == Search -> Uri.parse("${Telephony.MmsSms.SEARCH_URI}/$searchParams")
//            else -> throw Exception("Invalid MessageQueryEnum: $this")
//        }
//    }
//}

class MessageQuery(val context: Context) {

//    fun fetchAll(): List<Map<String, Any?>> {
//        val uriMms: Uri = MessagesFilter.AllMms.toUri()
//        val uriSms: Uri = MessagesFilter.AllSms.toUri()
//        val resultsMms: MutableList<Map<String, Any?>> = MessageQueryHelper(context).query(uriMms)
//         val resultsSms: MutableList<Map<String, Any?>> = MessageQueryHelper(context).query(uriSms)
//        val results: MutableList<Map<String, Any?>> = (resultsMms + resultsSms).toMutableList()
//        return results
//    }

    // TODO: Need to wire this up
    fun fetch(lookupId: String): Map<String, Any?> {
        val uri =
            Uri.Builder()
                .path("${ContactsContract.Contacts.CONTENT_LOOKUP_URI}/$lookupId")
                .build()
        return MessageQueryHelper(context).query(uri).first()
    }
}

class MessageQueryHelper(val context: Context) {

    fun query(uri: Uri): MutableList<Map<String, Any?>> {
        val contentResolver = context.contentResolver
        val contactList = mutableListOf<Map<String, Any?>>()
        Log.d("Contacts", " <<< Querying URI: $uri")

        val commonDataUrl: Uri = Mms.CONTENT_URI
            .buildUpon()
            .appendQueryParameter("VISIBLE_CONTACTS_ONLY", "true")
            .build()

        val commonDataCursor = contentResolver.query(commonDataUrl, null, null, null, null)
        val commonData = getAllCursorData(commonDataCursor!!)
        Log.d("Contacts", " <<< Common Data: ${commonData.size}")

        val rawContactCursor = contentResolver.query(uri, null, null, null, null)
        val data = getAllCursorData(rawContactCursor!!)
        for (row in data) {
            if (row.isEmpty()) {
                continue
            }

            // Fetch phone numbers for this contact
            val phoneNumbers = mutableListOf<String>()
            val phoneNumCursor =
                contentResolver.query(
                    Mms.CONTENT_URI,
                    null,
                    "${Mms.CONTENT_URI} = ?",
                    arrayOf(row["_id"].toString()),
                    null,
                    null
                )
            val phoneNumData = getAllCursorData(phoneNumCursor!!)
            printQueryResults("Contact", row)

            for (phoneNumRow in phoneNumData) {
                if (phoneNumRow.isNotEmpty()) {
                    Log.d("Contacts", " <<< PhoneNumRow: $phoneNumRow")
                    try {
                        val phoneNumber: String =
                            phoneNumRow[Mms.CONTENT_URI.toString()] as String
                        phoneNumbers.add(phoneNumber)
                    } catch (_: Exception) {}
                }
            }

            // Fetch Emails for this contact
            val emails = mutableListOf<String>()
            val emailCursor =
                contentResolver.query(
                    Mms.CONTENT_URI,
                    null,
                    "${Mms.CONTENT_URI} = ?",
                    arrayOf(row["_ID"].toString()),
                    null,
                    null
                )
            val emailData = getAllCursorData(emailCursor!!)

            for (emailRow in emailData) {
                val email = emailRow[Mms.CONTENT_URI.toString()].toString()
                emails.add(email)
            }

            val contact: HashMap<String, Any?> = hashMapOf(
                "externalId" to row["_id"].toString(),
                "name" to row["display_name"].toString(),
                "phoneNumbers" to phoneNumbers,
                "emailAddresses" to emails,
                "lastUpdated" to (row["contact_last_updated_timestamp"] as Long? ?: 0),
                "ringtone" to "",
                "primaryName" to row["display_name"].toString(),
                "alternativeName" to row["display_name_alt"].toString(),
                "hasPhoneNum" to true,
                "inVisibleGroup" to true,
                "isUserProfile" to false,
                "lookupKey" to row["lookup"].toString(),
                "phoneticName" to row["lookupKey"].toString(),
                "photoUri" to row["lookupKey"].toString(),
                "photoId" to row["lookupKey"].toString(),
                "photoThumbnailUri" to row["lookupKey"].toString(),
                "starred" to false,
                "error" to ""
            )
            Log.d("Contacts", " <<< Final Contact: $contact")
            Log.d("Contacts", " <<< externalId: ${contact["externalId"]}")
            Log.d("Contacts", " <<< name: ${contact["name"]}")
            Log.d("Contacts", " <<< phoneNumbers: ${contact["phoneNumbers"]}")
            Log.d("Contacts", " <<< emailAddresses: ${contact["emailAddresses"]}")
            Log.d("Contacts", " <<< lastUpdated: ${contact["lastUpdated"]}")
            Log.d("Contacts", " <<< ringtone: ${contact["ringtone"]}")
            Log.d("Contacts", " <<< primaryName: ${contact["primaryName"]}")
            Log.d("Contacts", " <<< alternativeName: ${contact["alternativeName"]}")
            Log.d("Contacts", " <<< hasPhoneNum: ${contact["hasPhoneNum"]}")
            Log.d("Contacts", " <<< inVisibleGroup: ${contact["inVisibleGroup"]}")
            Log.d("Contacts", " <<< isUserProfile: ${contact["isUserProfile"]}")
            Log.d("Contacts", " <<< lookupKey: ${contact["lookupKey"]}")
            Log.d("Contacts", " <<< phoneticName: ${contact["phoneticName"]}")
            Log.d("Contacts", " <<< photoUri: ${contact["photoUri"]}")
            Log.d("Contacts", " <<< photoId: ${contact["photoId"]}")
            Log.d("Contacts", " <<< photoThumbnailUri: ${contact["photoThumbnailUri"]}")
            Log.d("Contacts", " <<< starred: ${contact["starred"]}")
            Log.d("Contacts", " <<< error: ${contact["error"]}")

            contactList.add(contact)
        }
        return contactList
    }
}

fun parseMmsMessage(context: Context, msgMap: Map<String, Any?>): Map<String, Any?> {

    val msgId = msgMap.getOrDefault("_id", -1).toString()
    val messageUri = "content://mms/part".toUri()
    val peopleUri =
        Mms.Addr.getAddrUriForMessage(msgId)

    printQueryResults("MMS", msgMap)

    Log.d("parseMmsMessage", " <<< MMS People:")

    var sender: HashMap<String, Map<String, Any?>> = hashMapOf()
    var recipients: HashMap<String, Map<String, Any?>> = hashMapOf()

    val contactCursor = context.contentResolver.query(peopleUri, null, null, null, null)
    val contactMap: List<Map<String, Any?>> = getAllCursorData(contactCursor!!)
    Log.d("parseMmsMessage", " <<< contactMap Size: ${contactMap.size}")
    for (contact in contactMap) {

        printQueryResults("MMS Person", msgMap)

        val participant = participantToContact(context, contact)
        val value = contact["address"].toString()

        Log.d("parseMmsMessage", " <<< Contact Type: ${contact["type"]}")
        val isSender: Boolean = (contact["type"] as Long).toInt() == 137
        if (isSender) {
            sender.put(value, participant)
        } else {
            recipients.put(value, participant)
        }
    }
    Log.d("parseSmsMessage", "sender <<<  $sender")
    if (sender.isEmpty()) {
        throw Exception("Sender is null")
    }
    Log.d("parseMmsMessage", " <<<")
    Log.d("parseMmsMessage", " <<< ===============================")
    Log.d("parseMmsMessage", " <<<")

    // Get MMS Parts
    val mediaList = mutableListOf<Map<String, Any?>>()
    Log.d("parseMmsMessage", " <<< Retrieving Data")

    val messageCursor = context.contentResolver.query(messageUri, null, "mid = $msgId", null, null)
    val messageData: List<Map<String, Any?>> = getAllCursorData(messageCursor!!)

    var externalId = msgId
    var body = ""
    var isRead = (msgMap["read"] as Long? ?: 0) == 1L
    var msgBox = (msgMap["msg_box"] as Long? ?: 0).toInt()
    var isOutbound = (msgBox == 2)
    var failedAt = null
    var readAt = null
    // var deliveredAt = msgMap.getOrDefault("d_tm", "").toString()
    var receivedAt = msgMap.getOrDefault("date", null) as Long? ?: 0
    var threadId = msgMap["thread_id"].toString()
    var pdu = msgMap.getOrDefault("pdu", "").toString()
    var userData = msgMap.getOrDefault("user_data", "").toString()
    var simNum = msgMap.getOrDefault("sim_slot", 0) as Long? ?: 0
    var simStatus = null
//    var media = emptyList<Map<String, Any?>>()

    var status: MessageStatus
    var sentAt: Long
    if (isOutbound) {
        status = MessageStatus.SENT
        sentAt = msgMap.getOrDefault("date", null) as Long? ?: 0
    } else {
        status = MessageStatus.RECEIVED
        sentAt = msgMap.getOrDefault("date_sent", null) as Long? ?: 0
    }

    var index = 0
    for (data in messageData) {
        index += 1
        val partId = data["_id"].toString()
        val partUri: Uri = "content://mms/part/$msgId".toUri()
        val contentType = data["ct"].toString()
        when {
            contentType.equals("text/plain", ignoreCase = true) -> body = data["text"].toString()
            else -> {
                Log.w("parseMmsMessage", " <<< Unhandled Content Type: $contentType")
                Log.w("parseMmsMessage", " <<< PartId: $partId")
                Log.w("parseMmsMessage", " <<< PartUri: $partUri")
                for ((key, value) in data) {
                    Log.w("parseMmsMessage", " <<<  $key = '$value'")
                }
                mediaList.add(
                    hashMapOf(
                        "externalId" to partId,
                        "uri" to partUri.toString(),
                        "contentType" to contentType
                    )
                )
            }
        }
    }

    // Log.d("parseMmsMessage", " <<< Error: $e")
    // body = msgMap.getOrDefault("body", "") as String

    var message: Map<String, Any?> =
        mapOf<String, Any?>(
            "externalId" to externalId,
            "body" to body,
            "isRead" to isRead,
            "isOutbound" to isOutbound,
            "status" to status,
            "failedAt" to failedAt,
            "readAt" to readAt,
            "receivedAt" to receivedAt,
            "sentAt" to sentAt,
            "threadId" to threadId,
            "pdu" to pdu,
            "userData" to userData,
            "sender" to sender,
            "recipients" to recipients,
            "simNum" to simNum,
            "simStatus" to simStatus,
            "media" to mediaList,
        )

    if (sender.isEmpty()) {
        throw Exception("Sender is null")
    }
    return message
}

enum class MessageStatus {
    RECEIVING,
    RECEIVED,
    SENT

}

fun parseSmsMessage(context: Context, msgMap: Map<String, Any?>): Map<String, Any?> {
    // Log.d("parseTextMessage", " <<< ${msgs[0]}")
    // Log.d("MessageQuery", " <<< ***************************")

    val msgId = msgMap["_id"].toString()
    var msgBox = (msgMap["msg_box"] as Long? ?: 0).toInt()
    var isOutbound = (msgBox == 2)
    val senderId = msgMap.getOrDefault("_id", null).toString()
    val senderPhone = msgMap.getOrDefault("address", null).toString()

    // Get MMS Parts
    // val mediaList = mutableListOf<Map<String, Any?>>()

    Log.d("parseSmsMessage", " <<< MessageId: $msgId")
    Log.d("parseSmsMessage", " <<<")
    Log.d("parseSmsMessage", " <<< -------------------------------")
    Log.d("parseSmsMessage", " <<< ------------ SMS Data ----------")
    Log.d("parseSmsMessage", " <<< -------------------------------")

    Log.d("parseSmsMessage", " <<< SMS Message Size: ${msgMap.size}")
    for ((key, value) in msgMap) {
        Log.d("parseSmsMessage", "msgMap <<<  $key = '$value'")
    }
    val sender = hashMapOf(
        senderPhone.toString() to (ContactQuery(context).fetch(ContactsFilter.PhoneNumber, senderPhone)
            ?: mapOf(
                "externalId" to senderId,
                "name" to "",
                "phoneNumbers" to listOf(senderPhone),
                "emailAddresses" to listOf<String>(),
                "alternativeName" to "",
                "lookupKey" to "",
                "phoneticName" to "",
                "primaryName" to ""
            )
                )
    )
//    val messageCursor = context.contentResolver.query(messageUri, null, "mid = $msgId", null, null)
//    val messageData: List<Map<String, Any?>> = getAllCursorData(messageCursor!!)

    Log.d("parseSmsMessage", " <<< -------------------------------")
    Log.d("parseSmsMessage", "senderPhone <<<  $senderPhone")
    if (senderPhone.isBlank()) {
        throw Exception("Sender is null")
    }
    val message: Map<String, Any?> =
            mapOf<String, Any?>(
                "externalId" to msgId,
                "body" to msgMap.getOrDefault("body", "") as String,
                "isRead" to false,
                "isOutbound" to isOutbound,
                "status" to MessageStatus.RECEIVED,
                "failedAt" to null,
                "readAt" to null,
                "receivedAt" to msgMap.getOrDefault("date", 0) as Long,
                "sentAt" to msgMap.getOrDefault("date_sent", 0) as Long,
                "threadId" to msgMap.getOrDefault("thread_id", "").toString(),
                "pdu" to null,
                "userData" to null,
                "sender" to sender,
                "recipients" to hashMapOf<String, Any?>(),
                "simNum" to msgMap.getOrDefault("sim_slot", 0) as Long,
                "simStatus" to null,
                "media" to emptyList<Map<String, Any?>>(),
            )

    return message
}

fun participantToContact(context: Context, contactMap: Map<String, Any?>): Map<String, Any?> {
    val searchString = contactMap["address"].toString()

    Log.d("parseMmsMessage", " <<< address: $searchString")

    val phoneNum: String?
    val emailAddress: String?
    var participant: Map<String, Any?>?

    if (searchString.contains('@')) {
        emailAddress = searchString
        phoneNum = null
        participant = ContactQuery(context).fetch(ContactsFilter.Email, emailAddress)
    } else {
        phoneNum = searchString
        emailAddress = null
        participant = ContactQuery(context).fetch(ContactsFilter.PhoneNumber, searchString)
    }
    if (participant == null) {
        participant = hashMapOf(
            "externalId" to contactMap.getOrDefault("_id", "").toString(),
            "name" to "",
            "phoneNumbers" to listOf(phoneNum),
            "emailAddresses" to listOf(emailAddress),
            "alternativeName" to "",
            "lookupKey" to "",
            "phoneticName" to "",
            "primaryName" to ""
        )
    }
    return participant
}

private fun getAllCursorData(cursor: Cursor): List<Map<String, Any?>> {
    val returnable: MutableList<Map<String, Any?>> = mutableListOf()
    cursor.use {
        while (it.moveToNext()) {
            val row: HashMap<String, Any?> = HashMap()
            for (index in 0 until cursor.columnCount) {
                val columnName = cursor.getColumnName(index)
                val columnType = cursor.getType(index)
                when (columnType) {
                    Cursor.FIELD_TYPE_NULL -> row[columnName] = ""
                    Cursor.FIELD_TYPE_INTEGER -> row[columnName] = cursor.getLong(index)
                    Cursor.FIELD_TYPE_FLOAT -> row[columnName] = cursor.getFloat(index)
                    Cursor.FIELD_TYPE_STRING -> row[columnName] = cursor.getString(index)
                    Cursor.FIELD_TYPE_BLOB -> row[columnName] = cursor.getBlob(index)
                    else -> throw Exception("Unknown column type: $columnType")
                }
            }
            if (row.isNotEmpty()) {
                returnable.add(row)
            }
        }
    }
    return returnable
}

private fun printQueryResults(objectName: String, results: Map<String, Any?>) {
    val sortedMap = results.toSortedMap(compareBy<String> { it }.thenBy { it.length })

    android.util.Log.d("printQueryResults", " <<< ")
    android.util.Log.d("printQueryResults", " <<< -------------------------------")
    android.util.Log.d("printQueryResults", " <<<    >>>> $objectName Data ")
    android.util.Log.d("printQueryResults", " <<< -------------------------------")
    android.util.Log.d("printQueryResults", " <<< ")
    for (key in sortedMap.keys) android.util.Log.d("printQueryResults", " <<< $key: ${sortedMap[key]}")
    android.util.Log.d("printQueryResults", " <<< ")
    android.util.Log.d("printQueryResults", " <<< ")
    android.util.Log.d("printQueryResults", " <<< ")
}

