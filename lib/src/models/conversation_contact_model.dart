import 'package:simple_sms/src/models/query_obj.dart';


class AndroidConversationContactFields {
  // Android field names
  static const id = 'id';
  static const externalId = 'external_id';
  static const createdAt = 'created_at';
  static const modifiedAt = 'modified_at';
  static const conversationId = 'conversation_id';
  static const contactId = 'contact_id';

  // iOS field names
  static const iosId = '';
  static const iosExternalId = '';
  static const iosCreatedAt = '';
  static const iosModifiedAt = '';
  static const iosConversationId = '';
  static const iosContactId = '';
}

class AndroidConversationContact implements ConversationContactProvider {
  final String _conversationId;
  final String _contactId;

  AndroidConversationContact({
    required String conversationId,
    required String contactId,
  })  : _conversationId = conversationId,
        _contactId = contactId;

  @override
  String get conversationId => _conversationId;

  @override
  String get contactId => _contactId;

  @override
  ConversationContactsCompanion toDriftCompanion() {
    return ConversationContactsCompanion(
      conversationId: Value(conversationId),
      contactId: Value(contactId),
    );
  }

  @override
  factory AndroidConversationContact.fromDriftCompanion(
          ConversationContactsCompanion companion) =>
      AndroidConversationContact(
        conversationId: companion.conversationId.value,
        contactId: companion.contactId.value,
      );

  factory AndroidConversationContact.fromJson(Map<String, dynamic> json) =>
      AndroidConversationContact(
        conversationId: json['conversationId'],
        contactId: json['contactId'],
      );

  Map<String, dynamic> toJson() => {
        'conversationId': conversationId,
        'contactId': contactId,
      };
}
