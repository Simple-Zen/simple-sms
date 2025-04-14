import 'package:simple_sms/src/models/query_obj.dart';


enum AndroidMmsProvider implements EnumProvider {
  mms(
    contentUri: 'content://mms/',
    projection: [],
  ),
  mmsParts(
    contentUri: 'content://mms/part/',
    projection: [],
  ),
  ;

  const AndroidMmsProvider({
    required this.contentUri,
    required this.projection,
  });
  final String contentUri;
  final List<String> projection;

  @override
  Future<List<MessagesCompanion>> query([QueryObj? queryObj]) async {
    List<MessagesCompanion> companions = [];
    final uuid = const Uuid();

    final allParts = await queryParts(null);
    final messages = await CuriousPigeon().query(queryObj ??
        QueryObj(
          contentUri: contentUri,
          sortOrder: 'date ASC',
          projection: projection,
        ));

    for (final message in messages) {
      AndroidMms androidMms =
          AndroidMms.fromJson(Map<String, dynamic>.from(message));

      final List<AndroidMmsParts> parts = allParts[androidMms.externalId] ?? [];
      final List<AttachmentsCompanion> attachments = [];

      for (final part in parts) {
        final contentType = part.contentType.split('/')[0];
        final contentSubType = part.contentType.contains('/')
            ? part.contentType.split('/')[1]
            : '';

        switch (contentType) {
          case 'text':
            // For text parts, add to the body
            androidMms.body = '${androidMms.body} ${part.text}';
            break;
          case 'image':
          case 'audio':
          case 'video':
          case 'application':
          default:
            // For all other content types, create an attachment
            if (part.externalId.isNotEmpty) {
              final attachmentId = uuid.v4();

              // Create proper content URI for the attachment
              // Format: content://mms/part/{part_id}
              final String path = 'content://mms/part/${part.externalId}';

              // print('Creating MMS attachment: ${part.contentType} at $path');

              attachments.add(
                AttachmentsCompanion(
                  id: Value(attachmentId),
                  messageId:
                      Value(''), // Will be updated after message is saved
                  path: Value(path),
                  mimeType: Value(part.contentType),
                ),
              );
            }
            break;
        }
      }

      final companionToAdd = androidMms.toDriftCompanion();
      companions.add(companionToAdd);

      // Store attachments if there are any
      if (attachments.isNotEmpty) {
        try {
          // First ensure the message is in the database to get its ID
          final message = await db.messagesDao.upsert(companionToAdd);

          // print(
          //     'Saving ${attachments.length} attachments for message: ${message.id}');

          // Now add all attachments with the proper message ID
          for (var attachment in attachments) {
            await db.attachmentsDao
                .upsert(attachment.copyWith(messageId: Value(message.id)));
          }
        } catch (e) {
          print('Error saving MMS attachments: $e');
        }
      }
    }
    return companions;
  }

  Future<Map<String, List<AndroidMmsParts>>> queryParts(
      String? lookupKey) async {
    final partMap = <String, List<AndroidMmsParts>>{};
    final parts = await CuriousPigeon().query(QueryObj(
      contentUri: AndroidMmsProvider.mmsParts.contentUri,
      sortOrder: 'mid ASC, name ASC',
      projection: AndroidMmsProvider.mmsParts.projection,
    ));

    // print('Found ${parts.length} MMS parts');

    for (final part in parts) {
      AndroidMmsParts androidMmsParts =
          AndroidMmsParts.fromJson(Map<String, dynamic>.from(part));

      // print(
      //     'MMS part - ID: ${androidMmsParts.externalId}, Type: ${androidMmsParts.contentType}, MsgID: ${androidMmsParts.messageId}');

      partMap
          .putIfAbsent(androidMmsParts.messageId, () => [])
          .add(androidMmsParts);
    }
    return partMap;
  }
}

class AndroidMmsFields {
  // Android field names
  static const externalId = '_id';
  static const externalParentId = 'thread_id';
  static const body = 'body';
  static const textOnly = 'text_only';
  static const creator = 'creator';
  static const contentTypeClass = 'ct_cls';
  static const contentType = 'ct';
  static const contentLink = 'ct_l';
  static const receivedAt = 'date';
  static const sentAt = 'date_sent';
  static const messageType = 'm_type';
  static const messageBox = 'msg_box';
  static const priority = 'pri';
  static const read = 'read';
  static const isSeen = 'seen';
  static const simSlot = 'sim_slot';
  static const spamReport = 'spam_report';
  static const trId = 'tr_id';
  static const status = 'status';
  static const isRead = 'is_read';
  static const isOutbound = 'is_outbound';
}

class AndroidMms implements MessageProvider {
  AndroidMms({
    required this.externalId,
    required this.externalParentId,
    required this.contentLink,
    required this.messageType,
    required this.messageBox,
    required this.priority,
    required this.isSeen,
    required this.spamReport,
    required this.trId,
    required this.body,
    required this.conversationId,
    this.senderId,
    required this.simSlot,
    required this.sentAt,
    required this.receivedAt,
    required this.readAt,
    required this.failedAt,
    required this.isRead,
    required this.isOutbound,
    required this.status,
  });

  final MessageType type = MessageType.mms;

  @override
  DateTime? createdAt;

  @override
  DateTime? modifiedAt;

  @override
  String externalId;

  @override
  String externalParentId;

  @override
  String? contentLink;

  @override
  String? messageType;

  @override
  String? messageBox;

  @override
  String? priority;

  @override
  String? isSeen;

  @override
  String? spamReport;

  @override
  String? trId;

  @override
  String body;

  @override
  String conversationId;

  @override
  String? senderId;

  @override
  String simSlot;

  @override
  DateTime? sentAt;

  @override
  DateTime? receivedAt;

  @override
  DateTime? readAt;

  @override
  DateTime? failedAt;

  @override
  bool? isRead;

  @override
  bool? isOutbound;

  @override
  MessageStatus status;

  factory AndroidMms.fromJson(Map<String, dynamic> json) => AndroidMms(
        externalId: json[AndroidMmsFields.externalId].toString(),
        externalParentId: json[AndroidMmsFields.externalParentId].toString(),
        contentLink: json[AndroidMmsFields.contentLink],
        messageType: json[AndroidMmsFields.messageType].toString(),
        messageBox: json[AndroidMmsFields.messageBox].toString(),
        priority: json[AndroidMmsFields.priority].toString(),
        isSeen: json[AndroidMmsFields.isSeen].toString(),
        spamReport: json[AndroidMmsFields.spamReport].toString(),
        trId: json[AndroidMmsFields.trId].toString(),
        body: json[AndroidMmsFields.body] ?? '',
        simSlot: json[AndroidMmsFields.simSlot].toString(),
        sentAt: json[AndroidMmsFields.sentAt] > 0
            ? DateTime.fromMillisecondsSinceEpoch(
                (json[AndroidMmsFields.sentAt] * 1000))
            : json[AndroidMmsFields.receivedAt] > 0
                ? DateTime.fromMillisecondsSinceEpoch(
                    (json[AndroidMmsFields.receivedAt] * 1000))
                : null,
        receivedAt: json[AndroidMmsFields.receivedAt] > 0
            ? DateTime.fromMillisecondsSinceEpoch(
                (json[AndroidMmsFields.receivedAt] * 1000))
            : null,
        readAt: json[AndroidMmsFields.read] > 0
            ? DateTime.fromMillisecondsSinceEpoch(
                (json[AndroidMmsFields.read] * 1000))
            : null,
        isRead: json[AndroidMmsFields.isRead],
        conversationId: json[AndroidMmsFields.externalParentId].toString(),
        failedAt: null,
        isOutbound: json[AndroidMmsFields.messageType] == '128' ? true : false,
        status: json[AndroidMmsFields.messageType] == '128'
            ? MessageStatus.sent
            : MessageStatus.received,
      );

  Map<String, dynamic> toJson() => {
        AndroidMmsFields.externalId: externalId,
        AndroidMmsFields.body: body,
        AndroidMmsFields.messageType: messageType,
        AndroidMmsFields.messageBox: messageBox,
        AndroidMmsFields.priority: priority,
        AndroidMmsFields.isSeen: isSeen,
        AndroidMmsFields.spamReport: spamReport,
        AndroidMmsFields.trId: trId,
        AndroidMmsFields.externalParentId: externalParentId,
        AndroidMmsFields.simSlot: simSlot,
        AndroidMmsFields.sentAt: sentAt,
        AndroidMmsFields.receivedAt: receivedAt,
        AndroidMmsFields.isRead: isRead,
        AndroidMmsFields.isOutbound: isOutbound,
        AndroidMmsFields.status: status,
      };

  @override
  MessagesCompanion toDriftCompanion() {
    return MessagesCompanion(
      type: Value(type),
      externalId: Value(externalId),
      externalParentId: Value(externalParentId),
      body: Value(body),
      conversationId: Value(conversationId),
      senderId: Value(senderId),
      simSlot: Value(simSlot),
      sentAt: Value(sentAt ?? DateTime.now()),
      receivedAt: Value(receivedAt ?? DateTime.now()),
      readAt: Value(readAt ?? DateTime.now()),
      failedAt: Value(failedAt ?? DateTime.now()),
      isRead: Value(isRead ?? false),
      isOutbound: Value(isOutbound ?? false),
      status: Value(status),
    );
  }

  @override
  factory AndroidMms.fromDriftCompanion(MessagesCompanion companion) =>
      AndroidMms(
        externalId: companion.externalId.value,
        externalParentId: companion.externalParentId.value,
        contentLink: null,
        messageType: null,
        messageBox: null,
        priority: null,
        isSeen: null,
        spamReport: null,
        trId: null,
        body: companion.body.value,
        conversationId: companion.conversationId.value ?? '',
        senderId: companion.senderId.value ?? '',
        simSlot: companion.simSlot.value,
        sentAt: companion.sentAt.value,
        receivedAt: companion.receivedAt.value,
        readAt: companion.readAt.value,
        failedAt: companion.failedAt.value,
        isRead: companion.isRead.value,
        isOutbound: companion.isOutbound.value,
        status: companion.status.value,
      );
}

class AndroidMmsPartsFields {
  // Android field names
  static const externalId = '_id';
  static const contentDestination = 'cd';
  static const charSet = 'chset';
  static const contentId = 'cid';
  static const contentLocation = 'cl';
  static const contentType = 'ct';
  static const messageId = 'mid';
  static const name = 'name';
  static const text = 'text';
}

class AndroidMmsParts {
  AndroidMmsParts({
    required this.externalId,
    required this.contentDestination,
    required this.charSet,
    required this.contentId,
    required this.contentLocation,
    required this.contentType,
    required this.messageId,
    required this.name,
    required this.text,
  });
  String externalId;
  String contentDestination;
  String? charSet;
  String contentId;
  String contentLocation;
  String contentType;
  String messageId;
  String name;
  String text;

  factory AndroidMmsParts.fromJson(Map<String, dynamic> json) =>
      AndroidMmsParts(
        externalId: json[AndroidMmsPartsFields.externalId].toString(),
        contentDestination: json[AndroidMmsPartsFields.contentDestination],
        charSet: json[AndroidMmsPartsFields.charSet].toString(),
        contentId: json[AndroidMmsPartsFields.contentId],
        contentLocation: json[AndroidMmsPartsFields.contentLocation],
        contentType: json[AndroidMmsPartsFields.contentType],
        messageId: json[AndroidMmsPartsFields.messageId].toString(),
        name: json[AndroidMmsPartsFields.name],
        text: json[AndroidMmsPartsFields.text],
      );

  Map<String, dynamic> toJson() => {
        AndroidMmsPartsFields.externalId: externalId,
        AndroidMmsPartsFields.contentDestination: contentDestination,
        AndroidMmsPartsFields.charSet: charSet,
        AndroidMmsPartsFields.contentId: contentId,
        AndroidMmsPartsFields.contentLocation: contentLocation,
        AndroidMmsPartsFields.contentType: contentType,
        AndroidMmsPartsFields.messageId: messageId,
        AndroidMmsPartsFields.name: name,
        AndroidMmsPartsFields.text: text,
      };
}
