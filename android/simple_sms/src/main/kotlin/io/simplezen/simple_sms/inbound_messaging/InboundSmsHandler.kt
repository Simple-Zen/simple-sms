package io.simplezen.simple_sms.src

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.provider.Telephony.Threads.getOrCreateThreadId
import android.telephony.SmsManager
import android.util.Log
import com.simplezen.unify_messages_plus.src.InboundMessaging
import java.util.Date

// Inbound SMS Messages
class InboundSmsHandler() : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        // Check if the action string matches the expected value
        if (intent.action != "android.provider.Telephony.SMS_DELIVER") {
            Log.w("IncomingMmsHandler", " <<< Received unexpected action: ${intent.action}")
            return
        }
        // Log the incoming Intent
        Log.d("IncomingSmsHandler", " <<< Received SMS - $intent")
        Log.d("IncomingSmsHandler", " <<< Received SMS - ${intent.dataString}")
        Log.d("IncomingSmsHandler", " <<< Received SMS - ${intent.extras?.keySet()?.toList().toString()}")
        Log.d("IncomingSmsHandler", " <<< ")

        try {
            // Retrieve SMS messages from the Intent
            val intentMsg = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            Log.d("IncomingSmsHandler", " <<< Msg: ${intentMsg[0]}")

            val msgMap =
                    mapOf(
                            "_id" to (intent.extras?.getLong("messageId") ?: -1),
                            "body" to intentMsg.joinToString(" ") { it.displayMessageBody },
                            "date" to Date().time,
                            "date_sent" to intentMsg[0].timestampMillis,
                            "type" to 2,
                            "read" to (intentMsg[0].statusOnIcc == SmsManager.STATUS_ON_ICC_READ),
                            "pdu" to intentMsg[0].pdu.toString(),
                            "recipients" to emptyList<String>(),
                            "sender" to (intentMsg[0].originatingAddress ?: ""),
                            "address" to intentMsg[0].originatingAddress!!,
                            "simSlot" to
                                    (intent.extras?.getInt("android.telephony.extra.SLOT_INDEX")
                                            ?: -1),
                            "thread_id" to
                                    getOrCreateThreadId(context, intentMsg[0].originatingAddress)
                                            .toString(),
                            "message_type" to "sms",
                            "user_data" to intentMsg[0].userData.toString(),
                    )

            val sender = intentMsg[0].originatingAddress
            Log.d("IncomingSmsHandler", " <<< Msg Sender: $sender")


            val temp = intent.resolveActivityInfo(context.packageManager, 0)
            Log.d("IncomingSmsHandler", " <<< Msg Index: $temp")

            val msgDate: Long = intentMsg[0].timestampMillis
            Log.d("IncomingSmsHandler", " <<< Msg Date: $msgDate")

            val msgBody = intentMsg[0].displayMessageBody
            Log.d("IncomingSmsHandler", " <<< Msg Body: $msgBody")

            val msgThread = getOrCreateThreadId(context, intentMsg[0].originatingAddress)
            Log.d("IncomingSmsHandler", " <<< Msg Thread: $msgThread")

            InboundMessaging(context).receiveInboundMessage(msgMap)

        } catch (e: Exception) {
            Log.e("IncomingSmsHandler", " <<<<< Error: $e")
            Log.e("IncomingSmsHandler", " <<<<< Error: ${e.stackTraceToString()}")
        }
    }
}
