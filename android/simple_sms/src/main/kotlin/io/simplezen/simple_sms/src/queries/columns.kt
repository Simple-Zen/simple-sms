/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.simplezen.unify_messages_plus.src.queries

import android.content.Context
import android.provider.BaseColumns

class Columns() {
    object tables {

        // Table names
        const val CONVERSATIONS_TABLE: String = "conversations"
        const val MESSAGES_TABLE: String = "messages"
        const val PARTS_TABLE: String = "parts"
        const val PARTICIPANTS_TABLE: String = "content://com.android.contacts/contacts"
        const val CONVERSATION_PARTICIPANTS_TABLE: String = "conversation_participants"

        // Views
        const val DRAFT_PARTS_VIEW: String = "draft_parts_view"
    }

    // Conversations table schema
    object ConversationColumns : BaseColumns {
        /* SMS/MMS Thread ID from the system provider */
        const val SMS_THREAD_ID: String = "sms_thread_id"

        /* Display name for the conversation */
        const val NAME: String = "name"

        /* Latest Message ID for the read status to display in conversation list */
        const val LATEST_MESSAGE_ID: String = "latest_message_id"

        /* Latest text snippet for display in conversation list */
        const val SNIPPET_TEXT: String = "snippet_text"

        /* Latest text subject for display in conversation list, empty string if none exists */
        const val SUBJECT_TEXT: String = "subject_text"

        /* Preview Uri */
        const val PREVIEW_URI: String = "preview_uri"

        /* The preview uri's content type */
        const val PREVIEW_CONTENT_TYPE: String = "preview_content_type"

        /* If we should display the current draft snippet/preview pair or snippet/preview pair */
        const val SHOW_DRAFT: String = "show_draft"

        /* Latest draft text subject for display in conversation list, empty string if none exists*/
        const val DRAFT_SUBJECT_TEXT: String = "draft_subject_text"

        /* Latest draft text snippet for display, empty string if none exists */
        const val DRAFT_SNIPPET_TEXT: String = "draft_snippet_text"

        /* Draft Preview Uri, empty string if none exists */
        const val DRAFT_PREVIEW_URI: String = "draft_preview_uri"

        /* The preview uri's content type */
        const val DRAFT_PREVIEW_CONTENT_TYPE: String = "draft_preview_content_type"

        /* If this conversation is archived */
        const val ARCHIVE_STATUS: String = "archive_status"

        /* Timestamp for sorting purposes */
        const val SORT_TIMESTAMP: String = "sort_timestamp"

        /* Last read message timestamp */
        const val LAST_READ_TIMESTAMP: String = "last_read_timestamp"

        /* Avatar for the conversation. Could be for group of individual */
        const val ICON: String = "icon"

        /* Participant contact ID if this conversation has a single participant. -1 otherwise */
        const val PARTICIPANT_CONTACT_ID: String = "participant_contact_id"

        /* Participant lookup key if this conversation has a single participant. null otherwise */
        const val PARTICIPANT_LOOKUP_KEY: String = "participant_lookup_key"

        /*
         * Participant's normalized destination if this conversation has a single participant.
         * null otherwise.
         */
        const val OTHER_PARTICIPANT_NORMALIZED_DESTINATION: String =
            "participant_normalized_destination"

        /* Default self participant for the conversation */
        const val CURRENT_SELF_ID: String = "current_self_id"

        /* Participant count not including self (so will be 1 for 1:1 or bigger for group) */
        const val PARTICIPANT_COUNT: String = "participant_count"

        /* Should notifications be enabled for this conversation? */
        const val NOTIFICATION_ENABLED: String = "notification_enabled"

        /* Notification sound used for the conversation */
        const val NOTIFICATION_SOUND_URI: String = "notification_sound_uri"

        /* Should vibrations be enabled for the conversation's notification? */
        const val NOTIFICATION_VIBRATION: String = "notification_vibration"

        /* Conversation recipients include email address */
        const val INCLUDE_EMAIL_ADDRESS: String = "include_email_addr"

        // Record the last received sms's service center info if it indicates that the reply path
        // is present (TP-Reply-Path), so that we could use it for the subsequent message to send.
        // Refer to TS 23.040 D.6 and SmsMessageSender.java in Android Messaging app.
        const val SMS_SERVICE_CENTER: String = "sms_service_center"

        // A conversation is enterprise if one of the participant is a enterprise contact.
        const val IS_ENTERPRISE: String = "IS_ENTERPRISE"
    }

    // Messages table schema
    object MessageColumns : BaseColumns {
        /* conversation id that this message belongs to */
        const val CONVERSATION_ID: String = "conversation_id"

        /* participant which send this message */
        const val SENDER_PARTICIPANT_ID: String = "sender_id"

        /* This is bugle's internal status for the message */
        const val STATUS: String = "message_status"

        /* Type of message: SMS, MMS or MMS notification */
        const val PROTOCOL: String = "message_protocol"

        /* This is the time that the sender sent the message */
        const val SENT_TIMESTAMP: String = "sent_timestamp"

        /* Time that we received the message on this device */
        const val RECEIVED_TIMESTAMP: String = "received_timestamp"

        /* When the message has been seen by a user in a notification */
        const val SEEN: String = "seen"

        /* When the message has been read by a user */
        const val READ: String = "read"

        /* participant representing the sim which processed this message */
        const val SELF_PARTICIPANT_ID: String = "self_id"

        /*
         * Time when a retry is initiated. This is used to compute the retry window
         * when we retry sending/downloading a message.
         */
        const val RETRY_START_TIMESTAMP: String = "retry_start_timestamp"

        // Columns which map to the SMS provider
        /* Message ID from the platform provider */
        const val SMS_MESSAGE_URI: String = "sms_message_uri"

        /* The message priority for MMS message */
        const val SMS_PRIORITY: String = "sms_priority"

        /* The message size for MMS message */
        const val SMS_MESSAGE_SIZE: String = "sms_message_size"

        /* The subject for MMS message */
        const val MMS_SUBJECT: String = "mms_subject"

        /* Transaction id for MMS notificaiton */
        const val MMS_TRANSACTION_ID: String = "mms_transaction_id"

        /* Content location for MMS notificaiton */
        const val MMS_CONTENT_LOCATION: String = "mms_content_location"

        /* The expiry time (ms) for MMS message */
        const val MMS_EXPIRY: String = "mms_expiry"

        /* The detailed status (RESPONSE_STATUS or RETRIEVE_STATUS) for MMS message */
        const val RAW_TELEPHONY_STATUS: String = "raw_status"
    }

    // Parts table schema
    // A part may contain text or a media url, but not both.
    object PartColumns : BaseColumns {
        /* message id that this part belongs to */
        const val MESSAGE_ID: String = "message_id"

        /* conversation id that this part belongs to */
        const val CONVERSATION_ID: String = "conversation_id"

        /* text for this part */
        const val TEXT: String = "text"

        /* content uri for this part */
        const val CONTENT_URI: String = "uri"

        /* content type for this part */
        const val CONTENT_TYPE: String = "content_type"

        /* cached width for this part (for layout while loading) */
        const val WIDTH: String = "width"

        /* cached height for this part (for layout while loading) */
        const val HEIGHT: String = "height"

        /* de-normalized copy of timestamp from the messages table.  This is populated
         * via an insert trigger on the parts table.
         */
        const val TIMESTAMP: String = "timestamp"
    }

    // Participants table schema
    object ParticipantColumns : BaseColumns {
        /* The subscription id for the sim associated with this self participant.
         * Introduced in L. For earlier versions will always be default_sub_id (-1).
         * For multi sim devices (or cases where the sim was changed) single device
         * may have several different sub_id values */
        const val SUB_ID: String = "sub_id"

        /* The slot of the active SIM (inserted in the device) for this self-participant. If the
         * self-participant doesn't correspond to any active SIM, this will be
         * {@link android.telephony.SubscriptionManager#INVALID_SLOT_ID}.
         * The column is ignored for all non-self participants.
         */
        const val SIM_SLOT_ID: String = "sim_slot_id"

        /* The phone number stored in a standard E164 format if possible.  This is unique for a
         * given participant.  We can't handle multiple participants with the same phone number
         * since we don't know which of them a message comes from. This can also be an email
         * address, in which case this is the same as the displayed address */
        const val NORMALIZED_DESTINATION: String = "normalized_destination"

        /* The phone number as originally supplied and used for dialing. Not necessarily in E164
         * format or unique */
        const val SEND_DESTINATION: String = "send_destination"

        /* The user-friendly formatting of the phone number according to the region setting of
         * the device when the row was added. */
        const val DISPLAY_DESTINATION: String = "display_destination"

        /* A string with this participant's full name or a pretty printed phone number */
        const val FULL_NAME: String = "full_name"

        /* A string with just this participant's first name */
        const val FIRST_NAME: String = "first_name"

        /* A local URI to an asset for the icon for this participant */
        const val PROFILE_PHOTO_URI: String = "profile_photo_uri"

        /* Contact id for matching local contact for this participant */
        const val CONTACT_ID: String = "contact_id"

        /* String that contains hints on how to find contact information in a contact lookup */
        const val LOOKUP_KEY: String = "lookup_key"

        /* If this participant is blocked */
        const val BLOCKED: String = "blocked"

        /* The color of the subscription (FOR SELF PARTICIPANTS ONLY) */
        const val SUBSCRIPTION_COLOR: String = "subscription_color"

        /* The name of the subscription (FOR SELF PARTICIPANTS ONLY) */
        const val SUBSCRIPTION_NAME: String = "subscription_name"

        /* The exact destination stored in Contacts for this participant */
        const val CONTACT_DESTINATION: String = "contact_destination"
    }

    // Conversation Participants table schema - contains a list of participants excluding the user
    // in a given conversation.
    object ConversationParticipantsColumns : BaseColumns {
        /* participant id of someone in this conversation */
        const val PARTICIPANT_ID: String = "participant_id"

        /* conversation id that this participant belongs to */
        const val CONVERSATION_ID: String = "conversation_id"
    }


}