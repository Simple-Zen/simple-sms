package io.simplezen.simple_sms.device_actions

import android.content.Context

// KamikazeePigeon
class DestructiveActions(val context: Context) {

    fun deleteMessage(lookupId: String): Boolean {
        throw Exception("Not implemented")
    }

    fun deleteContact(lookupId: String): Boolean {
        throw Exception("Not implemented")
    }
}