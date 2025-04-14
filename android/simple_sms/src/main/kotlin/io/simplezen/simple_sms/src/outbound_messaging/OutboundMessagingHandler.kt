package com.simplezen.unify_messages_plus.src.outbound_messaging

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.net.toUri
import io.flutter.Log
import java.util.UUID

class OutboundMessagingHandler() : Service() {

    companion object {
    }

    // Ensure the device has a SIM card, otherwise return empty
    fun sendSms(context : Context, message: Map<String, Any?>): String {
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_MESSAGING)) {
            return PrivOutboundMessagingHandler(context).sendSms(message)
        } else {
            Log.d("SimsQuery", "Device missing FEATURE_TELEPHONY")
            return "Error: Device does not have a SIM card"
        }
    }

    // Ensure the device has a SIM card, otherwise return empty
    fun sendMms(context : Context, message: Map<Any, Any?>): String {
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_MESSAGING)) {
            return PrivOutboundMessagingHandler(context)
                    .sendMms("+16161616161", message.toString(), "")
        } else {
            Log.d("SimsQuery", "Device missing FEATURE_TELEPHONY")
            return "Error: Device does not have a SIM card"
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}

// Text Message Handler
private class PrivOutboundMessagingHandler(val context: Context) {

    fun sendSms(message: Map<String, Any?>): String {


        val address : String = message["recipients"].toString()
        val body : String = message["body"].toString()
        // val messageID = message["id"] as String
        // val subscriptionId = message["id"] as Int
        val tempId: Int = UUID.randomUUID().hashCode()

        val sentIntent = Intent("SMS_SENT").putExtra("messageID", tempId)
        val sentPendingIntent =
                PendingIntent.getBroadcast(context, 0, sentIntent, PendingIntent.FLAG_IMMUTABLE)
        Log.d("OutboundMessagingHandler", "Sent Intent: $sentIntent")
        Log.d("OutboundMessagingHandler", "Sent Intent Action: ${sentIntent.action}")
        context.registerReceiver(
            OutboundMessageStream(),
            IntentFilter(sentIntent.action),
            Context.RECEIVER_NOT_EXPORTED
        )

        val deliveredIntent = Intent("SMS_DELIVERED").putExtra("messageID", tempId)
        val deliveredPendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        UUID.randomUUID().hashCode(),
                        deliveredIntent,
                        PendingIntent.FLAG_IMMUTABLE
                )
        context.registerReceiver(
            OutboundMessageStream(),
            IntentFilter(deliveredIntent.action),
            Context.RECEIVER_NOT_EXPORTED
        )

        val smsManager =
        // if (subscriptionId > -1) {
        context.getSystemService(SmsManager::class.java)
        // } else {
        // context.getSystemService(SmsManager::class.java)
        // .createForSubscriptionId(subscriptionId)
        // }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                smsManager.sendTextMessage(
                        address,
                        null,
                        body,
                        sentPendingIntent,
                        deliveredPendingIntent,
                        tempId.toLong()
                )
            }
            Toast.makeText(context, "Message Sent", Toast.LENGTH_LONG).show()
            return ""
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to send message", Toast.LENGTH_LONG).show()
            return "Error: ${e.message}"
        }
    }

    fun sendMms(phoneNumber: String?, subject: String?, imagePath: String?): String {
        val intent =
                Intent(Intent.ACTION_SEND).apply {
//                    Intent.setType = "image/*" // Change to the type of media you want to send
                    putExtra("address", phoneNumber)
                    putExtra("sms_body", subject)
                    putExtra(
                            Intent.EXTRA_STREAM,
                        imagePath?.toUri()
                    ) // Replace with your image URI
                    putExtra("mms", true)
                }

        try {
            context.startActivity(Intent.createChooser(intent, "Send MMS"))
        } catch (_: Exception) {
            // Handle any exceptions
        }
        return ""
    }

//    fun getMessageStatus(lookupId: String): String {
//        val message = MessageQuery(context).fetch(lookupId)
//        return message["status"].toString()
//    }
}

private class OutboundMessageStream() : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("OutboundMessageStream", "Message Sent")
        Log.d("OutboundMessageStream", "Intent: $intent")
        // throw Exception("Not implemented")
    }

//    private val resultCodeMap: Map<Int, String> by lazy {
//        val map = mutableMapOf<Int, String>()
//        SmsManager::class.java.declaredFields.forEach { field ->
//            if (Modifier.isStatic(field.modifiers) && field.type == Int::class.javaPrimitiveType) {
//                try {
//                    field.isAccessible = true
//                    val value = field.getInt(null)
//                    map[value] = field.name
//                } catch (e: IllegalAccessException) {
//                    e.printStackTrace()
//                }
//            }
//        }
//        map
//    }
}
