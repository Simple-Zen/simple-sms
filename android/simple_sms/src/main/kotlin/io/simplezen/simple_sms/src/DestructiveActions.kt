package com.simplezen.unify_messages_plus.src

import android.content.Context

/*
    Houses all of the pigeons in Android
    Remember to add any new pigeons to [MainActivity] in the [configureFlutterEngine] method, ie:
        MessengerPigeon.setUp(binaryMessenger, Queries(context))
*/

// KamikazeePigeon
class DestructiveActions(val context: Context) : KamikazeePigeon {

    override fun deleteMessage(lookupId: String): Boolean {
        throw Exception("Not implemented")
    }

    override fun deleteContact(lookupId: String): Boolean {
        throw Exception("Not implemented")
    }
}
