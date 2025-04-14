import 'package:simple_sms/src/models/query_obj.dart';


enum AndroidConversationProvider implements EnumProvider {
  simpleConversations(
    contentUri: 'content://mms-sms/conversations?simple=true',
    projection: [],
  ),
  conversations(
    contentUri: 'content://mms-sms/conversations',
    projection: [],
  ),
  ;

  const AndroidConversationProvider({
    required this.contentUri,
    required this.projection,
  });
  final String contentUri;
  final List<String> projection;

  Future<List<AndroidConversation>> queryRaw() async {
    final conversations = await CuriousPigeon().query(
      QueryObj(
        contentUri: contentUri,
        sortOrder: 'date ASC',
        projection: projection,
      ),
    );
    List<AndroidConversation> finalConversations = conversations
        .map((conversation) => AndroidConversation.fromJson(
            Map<String, dynamic>.from(conversation)))
        .toList();

    return finalConversations;
  }

  @override
  Future<List<ConversationsCompanion>> query([QueryObj? queryObj]) async {
    List<ConversationsCompanion> companions = [];
    final conversations = await CuriousPigeon().query(queryObj ??
        QueryObj(
          contentUri: contentUri,
          sortOrder: 'date ASC',
          projection: projection,
        ));

    for (final conversation in conversations) {
      AndroidSimpleConversation androidConversation =
          AndroidSimpleConversation.fromJson(
              Map<String, dynamic>.from(conversation));
      companions.add(androidConversation.toDriftCompanion());
    }
    return companions;
  }
}

/// Android Conversation field names
class _SimpleConvoFields {
  static const externalId = '_id';
  static const isPinned = 'pin_to_top';
  static const isMuted = 'is_muted';
  static const recipientIds = 'recipient_ids';
  static const title = 'snippet';
  static const isArchived = 'archived';
  //Indicates if thread is common (0) or broadcast (1)
  static const type = 'type';

  ///Type classifier for messages in the thread
  ///
  /// 0: Traditional SMS - Standard text messages sent through cellular networks
  /// 1: MMS - Multimedia Messaging Service messages containing media
  /// 2: RCS (Rich Communication Services) - Enhanced messaging with rich features
  /// 3: System Message - Internal system notifications or status messages
  /// 4: Chat/IM - Internet-based instant messaging protocols
  /// 5: Emergency Messages - High-priority alert messages
  /// 6: Voicemail Notifications - Message threads containing voicemail notifications
  /// Manufacturer-Specific Extensions
  /// 7: Advanced Messaging (Samsung) - Samsung's enhanced messaging protocol
  /// 8: Chat+ (Carrier-specific) - Carrier-enhanced messaging services
  /// 9: Business Messages - Messages from verified business accounts
  /// 10: Auto-generated Messages - Automated system responses or OTP messages
  static const messageType = 'message_type';

  ///Flag indicating if messages are read
  static const isRead = 'read';

  ///Flag indicating if messages are verified as secure/safe
  static const isSafeMessage = 'safe_message';

  ///Classification of chat medium (SMS, MMS, RCS, etc.)
  static const chatType = 'chat_type';

  /// 0: Normal/Default View - Standard conversation display
  /// 1: Compact View - Condensed message bubbles with less spacing
  /// 2: Expanded View - Larger message bubbles with more content visible at once
  static const usingMode = 'using_mode';
}

class AndroidConversation implements ConversationProvider {
  AndroidConversation({
    required this.externalId,
    this.title,
    this.chatType,
    this.isArchived,
    this.isBlocked,
    this.isDeleted,
    this.isMuted,
    this.isPinned,
    this.isRead,
    this.messageType,
    this.recipientIds = const [],
    this.isSafeMessage,
    this.type,
    this.usingMode,
  });

  @override
  String? chatType;
  @override
  String? externalId;
  @override
  bool? isArchived;
  @override
  bool? isBlocked;
  @override
  bool? isDeleted;
  @override
  bool? isMuted;
  @override
  bool? isPinned;
  @override
  bool? isRead;
  @override
  String? messageType;
  @override
  List<String> recipientIds;
  @override
  bool? isSafeMessage;
  @override
  String? title;
  @override
  String? type;
  @override
  String? usingMode;

  /// Generates a display title for the conversation
  ///
  /// Priority:
  /// 1. Existing conversation title (if not null or empty)
  /// 2. Person names from people list (alphabetically sorted)
  /// 3. Contact values/phone numbers (sorted with alphabetic first, then numeric)
  static String generateDisplayTitle(
    Conversation conversation,
    List<Contact?> contacts,
    List<Person?> people,
  ) {
    // Priority 1: Use conversation title if available
    if (conversation.title != null && conversation.title!.isNotEmpty) {
      return conversation.title!;
    }

    // Priority 2 & 3: Build a list from participants (names or contact values)
    final participantsList = contacts
        .asMap()
        .entries
        .map((entry) {
          final idx = entry.key;
          final contact = entry.value;
          final person = idx < people.length ? people[idx] : null;

          if (person != null &&
              person.firstName.isNotEmpty &&
              person.lastName.isNotEmpty) {
            return '${person.firstName.trim()} ${person.lastName.trim()}';
          } else {
            return contact?.value ?? '';
          }
        })
        .where((name) => name.isNotEmpty)
        .toList();

    // Sort the list - names first, then numbers
    participantsList.sort((a, b) {
      // Check if both strings start with digits
      final aIsNumeric = RegExp(r'^\d').hasMatch(a);
      final bIsNumeric = RegExp(r'^\d').hasMatch(b);

      if (aIsNumeric && !bIsNumeric) {
        return 1; // Numbers come after letters
      } else if (!aIsNumeric && bIsNumeric) {
        return -1; // Letters come before numbers
      } else {
        return a.compareTo(b); // Standard alphanumeric sort
      }
    });

    return participantsList.join(', ');
  }

  @override
  ConversationsCompanion toDriftCompanion() {
    return ConversationsCompanion(
      title: Value(title),
      externalId: Value(externalId!),
      isArchived: Value(isArchived ?? false),
      isDeleted: Value(isDeleted ?? false),
      isMuted: Value(isMuted ?? false),
      isBlocked: Value(isBlocked ?? false),
    );
  }

  @override
  factory AndroidConversation.fromDriftCompanion(
          ConversationsCompanion companion) =>
      AndroidConversation(
        // title: companion.title.value ?? '',
        isArchived: companion.isArchived.value,
        isDeleted: companion.isDeleted.value,
        isMuted: companion.isMuted.value,
        isBlocked: companion.isBlocked.value,
        externalId: companion.id.value,
      );

  factory AndroidConversation.fromJson(Map<String, dynamic> json) =>
      AndroidConversation(
        // title: json[_SimpleConvoFields.title],
        externalId: json[_SimpleConvoFields.externalId].toString(),
        isArchived: false,
        isDeleted: false,
        isRead: json[_SimpleConvoFields.isRead] == 1 ? true : false,
        isMuted: json[_SimpleConvoFields.isMuted] == 1 ? true : false,
        isPinned: json[_SimpleConvoFields.isPinned] == 1 ? true : false,
        isBlocked: false,
        chatType: _chatTypeParser[json[_SimpleConvoFields.chatType]],
        messageType: _messageTypeParser[json[_SimpleConvoFields.messageType]],
        recipientIds: json[_SimpleConvoFields.recipientIds]?.split(' ') ?? [],
        isSafeMessage:
            json[_SimpleConvoFields.isSafeMessage] == 1 ? true : false,
        type: _typeParser[json[_SimpleConvoFields.type]],
        usingMode: _usingModeParser[json[_SimpleConvoFields.usingMode]],
      );

  Map<String, dynamic> toJson() => {
        // _SimpleConvoFields.title: title,
        _SimpleConvoFields.chatType: _chatTypeParser.entries
            .firstWhere((element) => element.value == chatType)
            .key,
        _SimpleConvoFields.externalId: externalId,
        _SimpleConvoFields.isArchived: isArchived,
        _SimpleConvoFields.isPinned: isPinned,
        _SimpleConvoFields.isMuted: isMuted,
        _SimpleConvoFields.isRead: isRead,
        _SimpleConvoFields.messageType: _messageTypeParser.entries
            .firstWhere((element) => element.value == messageType)
            .key,
        _SimpleConvoFields.recipientIds: recipientIds.join(' '),
        _SimpleConvoFields.isSafeMessage: isSafeMessage,
        _SimpleConvoFields.type: _typeParser.entries
            .firstWhere((element) => element.value == type)
            .key,
        _SimpleConvoFields.usingMode: _usingModeParser.entries
            .firstWhere((element) => element.value == usingMode)
            .key,
      };

  static const _chatTypeParser = <int, String>{
    0: 'normal',
    1: 'compact',
    2: 'expanded',
  };

  static const _messageTypeParser = <int, String>{
    0: 'traditional_sms',
    1: 'mms',
    2: 'rcs',
    3: 'system_message',
    4: 'chat_im',
    5: 'emergency_messages',
    6: 'voicemail_notifications',
    7: 'advanced_messaging',
    8: 'chat_plus',
    9: 'business_messages',
    10: 'auto_generated_messages',
  };

  static const _typeParser = <int, String>{
    0: 'normal',
    1: 'broadcast',
  };

  static const _usingModeParser = <int, String>{
    0: 'normal',
    1: 'compact',
    2: 'expanded',
  };
}

class AndroidSimpleConversation implements ConversationProvider {
  AndroidSimpleConversation({
    required this.externalId,
    this.title,
    this.recipientIds = const [],
    this.chatType,
    this.isArchived,
    this.isBlocked,
    this.isDeleted,
    this.isMuted,
    this.isPinned,
    this.isRead,
    this.messageType,
    this.isSafeMessage,
    this.type,
    this.usingMode,
  });

  @override
  String? chatType;
  @override
  String? externalId;
  @override
  bool? isArchived;
  @override
  bool? isBlocked;
  @override
  bool? isDeleted;
  @override
  bool? isMuted;
  @override
  bool? isPinned;
  @override
  bool? isRead;
  @override
  String? messageType;
  @override
  List<String> recipientIds;
  @override
  bool? isSafeMessage;
  @override
  String? title;
  @override
  String? type;
  @override
  String? usingMode;

  @override
  ConversationsCompanion toDriftCompanion() {
    return ConversationsCompanion(
      title: Value(title),
      externalId: Value(externalId!),
      isArchived: Value(isArchived ?? false),
      isDeleted: Value(isDeleted ?? false),
      isMuted: Value(isMuted ?? false),
      isBlocked: Value(isBlocked ?? false),
    );
  }

  @override
  factory AndroidSimpleConversation.fromDriftCompanion(
          ConversationsCompanion companion) =>
      AndroidSimpleConversation(
        // title: companion.title.value ?? '',
        isArchived: companion.isArchived.value,
        isDeleted: companion.isDeleted.value,
        isMuted: companion.isMuted.value,
        isBlocked: companion.isBlocked.value,
        externalId: companion.id.value,
        recipientIds: [],
      );

  factory AndroidSimpleConversation.fromJson(Map<String, dynamic> json) =>
      AndroidSimpleConversation(
        // title: json[_SimpleConvoFields.title],
        externalId: json[_SimpleConvoFields.externalId].toString(),
        isArchived: false,
        isDeleted: false,
        isRead: json[_SimpleConvoFields.isRead] == 1 ? true : false,
        isMuted: json[_SimpleConvoFields.isMuted] == 1 ? true : false,
        isPinned: json[_SimpleConvoFields.isPinned] == 1 ? true : false,
        isBlocked: false,
        chatType: _chatTypeParser[json[_SimpleConvoFields.chatType]],
        messageType: _messageTypeParser[json[_SimpleConvoFields.messageType]],
        recipientIds: json[_SimpleConvoFields.recipientIds].split(' '),
        isSafeMessage:
            json[_SimpleConvoFields.isSafeMessage] == 1 ? true : false,
        type: _typeParser[json[_SimpleConvoFields.type]],
        usingMode: _usingModeParser[json[_SimpleConvoFields.usingMode]],
      );

  Map<String, dynamic> toJson() => {
        _SimpleConvoFields.title: title,
        _SimpleConvoFields.chatType: _chatTypeParser.entries
            .firstWhere((element) => element.value == chatType)
            .key,
        _SimpleConvoFields.externalId: externalId,
        _SimpleConvoFields.isArchived: isArchived,
        _SimpleConvoFields.isPinned: isPinned,
        _SimpleConvoFields.isMuted: isMuted,
        _SimpleConvoFields.isRead: isRead,
        _SimpleConvoFields.messageType: _messageTypeParser.entries
            .firstWhere((element) => element.value == messageType)
            .key,
        _SimpleConvoFields.recipientIds: recipientIds.join(' '),
        _SimpleConvoFields.isSafeMessage: isSafeMessage,
        _SimpleConvoFields.type: _typeParser.entries
            .firstWhere((element) => element.value == type)
            .key,
        _SimpleConvoFields.usingMode: _usingModeParser.entries
            .firstWhere((element) => element.value == usingMode)
            .key,
      };

  static const _chatTypeParser = <int, String>{
    0: 'normal',
    1: 'compact',
    2: 'expanded',
  };

  static const _messageTypeParser = <int, String>{
    0: 'traditional_sms',
    1: 'mms',
    2: 'rcs',
    3: 'system_message',
    4: 'chat_im',
    5: 'emergency_messages',
    6: 'voicemail_notifications',
    7: 'advanced_messaging',
    8: 'chat_plus',
    9: 'business_messages',
    10: 'auto_generated_messages',
  };

  static const _typeParser = <int, String>{
    0: 'normal',
    1: 'broadcast',
  };

  static const _usingModeParser = <int, String>{
    0: 'normal',
    1: 'compact',
    2: 'expanded',
  };
}
