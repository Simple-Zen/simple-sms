package io.simplezen.simple_sms.src.device_actions

// KamikazeePigeon
class DestructiveActions(val context: Context) : KamikazeePigeon {

    override fun deleteMessage(lookupId: String): Boolean {
        throw Exception("Not implemented")
    }

    override fun deleteContact(lookupId: String): Boolean {
        throw Exception("Not implemented")
    }
}