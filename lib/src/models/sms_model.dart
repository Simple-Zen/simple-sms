import 'package:simple_sms/src/models/query_obj.dart';

class SmsFields {
  // Android field names
  static const externalId = '_id';
  static const externalParentId = 'thread_id';
  static const body = 'body';
  static const isOutbound = 'type';
  static const isRead = 'read';
  static const receivedAt = 'date';
  static const senderAddress = 'address';
  static const sentAt = 'date_sent';
  static const simSlot = 'sim_slot';
  static const status = 'status';
}

class AndroidSms {
  AndroidSms({
    required this.body,
    required this.externalId,
    required this.externalParentId,
    required this.receivedAt,
    required this.senderAddress,
    required this.sentAt,
    required this.simSlot,
    required this.status,
    this.conversationId,
    this.isOutbound,
    this.isRead,
    this.senderId,
  });

  final MessageType type = MessageType.sms;
  String? conversationId;
  String? senderId;
  String externalId;
  String externalParentId;
  String body;
  bool? isOutbound;
  bool? isRead;
  DateTime receivedAt;
  String senderAddress;
  DateTime sentAt;
  String simSlot;
  String status;

  factory AndroidSms.fromJson(Map<String, dynamic> json) => AndroidSms(
    body: json[AndroidSmsFields.body],
    externalId: json[AndroidSmsFields.externalId].toString(),
    externalParentId: json[AndroidSmsFields.externalParentId].toString(),
    isOutbound: json[AndroidSmsFields.isOutbound] == 2,
    isRead: json[AndroidSmsFields.isRead] == 1,
    receivedAt: DateTime.fromMillisecondsSinceEpoch(
      json[AndroidSmsFields.receivedAt],
    ),
    senderAddress: json[AndroidSmsFields.senderAddress].toString(),
    sentAt: DateTime.fromMillisecondsSinceEpoch(json[AndroidSmsFields.sentAt]),
    simSlot: json[AndroidSmsFields.simSlot].toString(),
    status: json[AndroidSmsFields.status].toString(),
  );

  Map<String, dynamic> toJson() => {
    AndroidSmsFields.body: body,
    AndroidSmsFields.externalId: externalId,
    AndroidSmsFields.externalParentId: externalParentId,
    AndroidSmsFields.isOutbound: isOutbound ?? false ? 2 : 1,
    AndroidSmsFields.isRead: isRead ?? false ? 1 : 0,
    AndroidSmsFields.receivedAt: receivedAt.millisecondsSinceEpoch,
    AndroidSmsFields.senderAddress: senderAddress,
    AndroidSmsFields.sentAt: sentAt.millisecondsSinceEpoch,
    AndroidSmsFields.simSlot: simSlot,
    AndroidSmsFields.status: status,
  };

  MessagesCompanion toDriftCompanion() {
    return MessagesCompanion(
      type: Value(type),
      externalId: Value(externalId),
      conversationId: Value(conversationId ?? ''),
      externalParentId: Value(externalParentId),
      body: Value(body),
      senderId: Value(senderId ?? ''),
      simSlot: Value(simSlot),
      sentAt: Value(sentAt),
      receivedAt: Value(receivedAt),
      isRead: Value(isRead ?? false),
      isOutbound: Value(isOutbound ?? false),
      status: Value(MessageStatus.sent),
    );
  }

  @override
  factory AndroidSms.fromDriftCompanion(MessagesCompanion companion) =>
      AndroidSms(
        externalId: companion.externalId.value,
        externalParentId: companion.externalParentId.value,
        body: companion.body.value,
        senderAddress: '',
        senderId: companion.senderId.value,
        simSlot: companion.simSlot.value,
        sentAt: companion.sentAt.value!,
        receivedAt: companion.receivedAt.value!,
        isRead: companion.isRead.value,
        isOutbound: companion.isOutbound.value,
        status: companion.status.value.toString(),
        conversationId: companion.conversationId.value,
      );
}
