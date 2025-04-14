package com.simplezen.unify_messages_plus.src

import android.content.Context
import com.simplezen.unify_messages_plus.MainActivity
import io.flutter.Log

// HomingPigeon
class InboundMessaging(val context: Context) {
    private var incomingPigeon: IncomingPigeon? = null

    // Get the existing FlutterEngine from MainActivity
    private fun setupPigeon() {
        try {
            // Only set up the pigeon if it hasn't been initialized yet
            if (incomingPigeon == null) {
                // Use existing BinaryMessenger from MainActivity's companion object
                val engine = MainActivity.getFlutterEngine()

                if (engine != null) {
                    val messenger = engine.dartExecutor.binaryMessenger
                    incomingPigeon = IncomingPigeon(messenger)
                    Log.d("InboundMessaging", "Successfully set up IncomingPigeon with existing engine")
                } else {
                    Log.e("InboundMessaging", "Failed to get existing FlutterEngine")
                }
            }
        } catch (e: Exception) {
            Log.e("InboundMessaging", "Error setting up pigeon: ${e.message}")
            e.printStackTrace()
        }
    }

    fun receiveInboundMessage(message: Map<String, Any?>) {
        // Set up the pigeon if not already done
        setupPigeon()

        // Check if pigeon is available
        if (incomingPigeon == null) {
            Log.e("receiveInboundMessage", "IncomingPigeon not initialized, cannot deliver message")
            return
        }

        // Perform the send operation
        Log.d("receiveInboundMessage", "Message received: $message")

        try {
            incomingPigeon?.receiveInboundMessage(message) { echo: Result<Any?> ->
                if (echo.isSuccess) {
                    Log.d("receiveInboundMessage", "Message received successfully")
                } else {
                    Log.e("receiveInboundMessage", "Failed to receive message: ${echo.exceptionOrNull()}")
                }
            }
        } catch (e: Exception) {
            Log.e("receiveInboundMessage", "Error sending message: ${e.message}")
            e.printStackTrace()
        }
    }
}

// TODO: Verify Country Locale w/ TelephonyManager