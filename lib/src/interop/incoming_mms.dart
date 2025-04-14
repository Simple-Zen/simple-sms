import 'package:flutter/foundation.dart';
import 'package:flutter/foundation.dart';

/// Handler for incoming SMS/MMS messages
@pragma('vm:entry-point')
class IncomingMms {
  Future<bool> receiveInboundMessage(
    Map<String, dynamic> inboundMessage,
  ) async {
    debugPrint('Received Message: ${inboundMessage['body']}');

    String? messageId;

    if (inboundMessage["message_type"] == "sms") {
      AndroidSms message = AndroidSms.fromJson(inboundMessage);
      debugPrint('Message: $message');

      message.conversationId =
          (await (db.conversations.select()..where(
                    (c) => c.externalId.equals(message.externalParentId),
                  ))
                  .getSingleOrNull())
              ?.id;

      if (message.conversationId == null) {
        final conversation = await db.conversationsDao.upsert(
          ConversationsCompanion(externalId: Value(message.externalParentId)),
        );
        message.conversationId = conversation.id;
      }

      final companion = message.toDriftCompanion();
      final savedMessage = await db.messagesDao.upsert(companion);
      messageId = savedMessage.id;
    } else if (inboundMessage["type"] == "mms") {
      AndroidMms message = AndroidMms.fromJson(inboundMessage);
      final companion = message.toDriftCompanion();
      final savedMessage = await db.messagesDao.upsert(companion);
      messageId = savedMessage.id;
    } else {
      throw Exception(
        'Unknown message type: ${inboundMessage.keys.toList().toString()}',
      );
    }

    // Process any attachments that came with the message
    if (messageId != null && inboundMessage.containsKey('attachments')) {
      final attachments = inboundMessage['attachments'];
      if (attachments is List && attachments.isNotEmpty) {
        for (final attachment in attachments) {
          if (attachment is Map<String, dynamic>) {
            final String path = attachment['path'] ?? '';
            final String mimeType =
                attachment['mimeType'] ?? 'application/octet-stream';

            if (path.isNotEmpty) {
              final attachmentCompanion = AttachmentsCompanion(
                id: Value(const Uuid().v4()),
                messageId: Value(messageId),
                path: Value(path),
                mimeType: Value(mimeType),
              );

              debugPrint('Processed attachment: $path with type $mimeType');
            }
          }
        }
      }
    }

    return true;
  }
}
