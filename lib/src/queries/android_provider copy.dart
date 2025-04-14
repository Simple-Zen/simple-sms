import 'dart:core';
import 'dart:async';
import 'package:uuid/uuid.dart';

// Flutter
import 'package:drift/drift.dart';
import 'package:flutter/services.dart';
import 'package:get/utils.dart';
import 'package:unify_messages_plus/src/providers/android/models/android_contacts.dart';

// Internal
import '../../../../repository/enums/message_enums.dart';
import '../models/conversation_model.dart';
import '../models/device_model.dart';
import '../models/person_model.dart';
import '../models/mms_model.dart';
import '../models/sms_model.dart';
import '../models/sim_model.dart';
import '../../../../services/app_logger.dart';
import '../../../../repository/repository.dart';
import '../../pigeon.g.dart';

//! All queries are to be done on their respective providers, not within this parent provider

/// Represents different intentions for permission requests
enum Intention {
  texting,
  calling,
  contacts,
  device,
  fileAccess;

  /// Get the Android role associated with this intention
  String? get role {
    switch (this) {
      case Intention.texting:
        return 'android.app.role.SMS';
      case Intention.calling:
        return 'android.app.role.DIALER';
      case Intention.contacts:
      case Intention.device:
      case Intention.fileAccess:
        return null;
    }
  }

  /// Get the Android permissions associated with this intention
  List<String> get permissions {
    switch (this) {
      case Intention.texting:
        return [
          'android.permission.SEND_SMS',
          'android.permission.READ_SMS',
          'android.permission.RECEIVE_SMS',
          'android.permission.WRITE_SMS',
          'android.permission.RECEIVE_WAP_PUSH',
          'android.permission.RECEIVE_MMS',
        ];
      case Intention.calling:
        return [
          'android.permission.READ_PHONE_STATE',
          'android.permission.READ_PHONE_NUMBERS',
        ];
      case Intention.contacts:
        return [
          'android.permission.WRITE_CONTACTS',
          'android.permission.READ_CONTACTS',
          'android.permission.MANAGE_OWN_CALLS',
        ];
      case Intention.device:
        return [
          'Manifest.permission.READ_DEVICE_CONFIG',
        ];
      case Intention.fileAccess:
        return [
          'android.permission.READ_EXTERNAL_STORAGE',
          'android.permission.READ_MEDIA_IMAGES',
          'android.permission.READ_MEDIA_VIDEO',
          'android.permission.READ_MEDIA_AUDIO',
        ];
    }
  }
}

final _sargentPigeon = SargentPigeon();
final _kamikazeePigeon = KamikazeePigeon();

// Message status streams
final StreamController<Message> _sentStatusStream =
    StreamController<Message>.broadcast();
Stream<Message> get onSentStatusChanged => _sentStatusStream.stream;

final StreamController<MessageStatus> _smsStateStreamController =
    StreamController<MessageStatus>.broadcast();
Stream<MessageStatus> get onStateChanged => _smsStateStreamController.stream;

/// Main entry point for Android-specific functionality
class AndroidProvider {
  static AndroidPermissions get permissions => _AndroidPermissions();
  static AndroidCommands get command => _AndroidCommands();
  static AndroidDestructiveCommands get destroy =>
      _AndroidDestructiveCommands();
  static AndroidQueries get query => _AndroidQueries(sync: false);
  static AndroidQueries get sync => _AndroidQueries(sync: true);

  /// Provides a way to debug MMS attachments by logging details of MMS parts
  static Future<void> debugMmsAttachments() async {
    try {
      print('Debugging MMS attachments...');

      // Query all MMS messages
      final mmsMessages = await CuriousPigeon().query(QueryObj(
        contentUri: AndroidMmsProvider.mms.contentUri,
        sortOrder: 'date DESC',
        projection: AndroidMmsProvider.mms.projection,
      ));

      print('Found ${mmsMessages.length} MMS messages');

      // For each MMS message, query its parts
      for (final mmsMessage in mmsMessages.take(5)) {
        // Limit to the first 5 messages for brevity
        final messageId = mmsMessage['_id'].toString();
        print('\nMMS Message ID: $messageId');

        // Query parts for this message
        final parts = await CuriousPigeon().query(QueryObj(
          contentUri: AndroidMmsProvider.mmsParts.contentUri,
          selection: 'mid = ?',
          selectionArgs: [messageId],
          sortOrder: 'name ASC',
          projection: [],
        ));

        print('  Found ${parts.length} parts for message $messageId');

        // Log details of each part
        for (final part in parts) {
          final partMap = Map<String, dynamic>.from(part);
          final partId = partMap['_id'].toString();
          final contentType = partMap['ct'] as String? ?? 'unknown';
          final name = partMap['name'] as String? ?? 'unnamed';
          final text = partMap['text'] as String? ?? '';

          print('  Part ID: $partId');
          print('    Content Type: $contentType');
          print('    Name: $name');
          if (contentType.startsWith('text/')) {
            print(
                '    Text: ${text.length > 30 ? "${text.substring(0, 30)}..." : text}');
          } else {
            print('    Binary content (${contentType})');
          }

          // For non-text parts, try to get content URI information
          if (!contentType.startsWith('text/')) {
            final uri = 'content://mms/part/$partId';
            print('    Access URI: $uri');
          }
        }
      }

      print('\nMMS attachment debugging complete');
    } catch (e) {
      print('Error debugging MMS attachments: $e');
    }
  }
}

/// Extension for query operations
abstract interface class AndroidQueries {
  Future<DevicesCompanion> deviceInfo([bool overrideSync = false]);
  Future<List<SimCardsCompanion>> allSimCards([bool overrideSync = false]);
  Future<List<SimCardsCompanion>> deviceSimCards([bool overrideSync = false]);
  Future<List<ConversationsCompanion>> allConversations(
      [bool overrideSync = false]);
  Future<List<ConversationContactsCompanion>> allConversationContacts(
      [bool overrideSync = false]);
  Future<List<MessagesCompanion>> allMessages({
    DateTime? since,
    String? threadId,
    String? address,
    bool? overrideSync = false,
  });
  Future<List<PeopleCompanion>> allPeople([bool overrideSync = false]);
  Future<List<ContactsCompanion>> allContacts([bool overrideSync = false]);
  Future<List<ContactsCompanion>> allContactsFromMessages(
      [bool overrideSync = false]);
}

class _AndroidQueries implements AndroidQueries {
  _AndroidQueries({required this.sync});
  final bool sync;

  /// Get device information
  @override
  Future<DevicesCompanion> deviceInfo([bool overrideSync = false]) async {
    print('Getting device info');
    final DevicesCompanion device = await AndroidDeviceProvider().query();

    return overrideSync == true || sync == true
        ? (await db.devicesDao.upsert(device)).toCompanion(false)
        : device;
  }

  /// Get SIM cards from device info
  @override
  Future<List<SimCardsCompanion>> deviceSimCards(
      [bool overrideSync = false]) async {
    print('Getting SIM cards from device info');
    final deviceId = (await deviceInfo(true)).id.value;

    // Get the device info which contains SIM card information
    Map<String, Object?> device = {};
    try {
      device = await CuriousPigeon().getDeviceInfo();
    } catch (e) {
      print('Error getting device info for SIM cards: $e');
      return []; // Return empty list if we can't get device info
    }

    // Check if 'sims' key exists and extract SIM cards
    final List<Map<String, dynamic>> simCardsData = [];
    if (device.containsKey('sims') && device['sims'] is List) {
      for (var sim in device['sims'] as List) {
        if (sim is Map) {
          simCardsData.add(Map<String, dynamic>.from(sim));
        }
      }
    }

    print('Found ${simCardsData.length} SIM cards in device info');

    // Process SIM cards and convert to SimCardsCompanion objects
    final List<SimCardsCompanion> simCards = AndroidDeviceProvider()
        .simCardsToDriftCompanions(deviceId, simCardsData);

    final finalSimCards = <SimCardsCompanion>[];
    for (var sim in simCards) {
      // Print the SIM card info for debugging
      print('Processing SIM card: slot=${sim.slot.value}, ' +
          'phoneNumber=${sim.phoneNumber.value}, ' +
          'imei=${sim.imei.value}');

      // Add the device ID to each SIM card
      final simWithDevice = sim.copyWith(deviceId: Value(deviceId));

      if (overrideSync == true || sync == true) {
        try {
          final result = await db.simCardsDao.upsert(simWithDevice);
          finalSimCards.add(result.toCompanion(false));
          print('Successfully saved SIM card to database');
        } catch (e) {
          print('Error upserting SIM card: $e');
          finalSimCards.add(simWithDevice);
        }
      } else {
        finalSimCards.add(simWithDevice);
      }
    }

    return finalSimCards;
  }

  /// Get information about all SIM cards
  @override
  Future<List<SimCardsCompanion>> allSimCards(
      [bool overrideSync = false]) async {
    final sims = await AndroidSimCardProvider().query();

    final finalSimCards = <SimCardsCompanion>[];
    for (var sim in sims) {
      overrideSync == true || sync == true
          ? finalSimCards
              .add((await db.simCardsDao.upsert(sim)).toCompanion(false))
          : finalSimCards.add(sim);
    }
    return finalSimCards;
  }

  @override
  Future<List<ConversationsCompanion>> allConversations(
      [bool overrideSync = false]) async {
    print('Getting all conversations');
    final List<ConversationsCompanion> conversations =
        await AndroidConversationProvider.simpleConversations.query();

    List<ConversationsCompanion> updatedConversations = [];
    for (final ConversationsCompanion companion in conversations) {
      overrideSync == true || sync == true
          ? updatedConversations.add(
              (await db.conversationsDao.upsert(companion)).toCompanion(false))
          : updatedConversations.add(companion);
    }
    return updatedConversations;
  }

  @override
  Future<List<ConversationContactsCompanion>> allConversationContacts(
      [bool overrideSync = false]) async {
    print('Getting all conversation contacts');
    // Valid IDs are required to pair the conversation with the conversation contact

    final people = await allPeople(true);
    final contacts = await allContacts(true);
    final conversations = await allConversations(true);
    final messages = await allMessages(overrideSync: true);

    // Get the device's phone number(s) to exclude the owner from conversation contacts
    final Set<String> devicePhoneNumbers = await _getDeviceOwnerPhoneNumbers();
    print('Device phone numbers: $devicePhoneNumbers');

    // Link messages to Contacts
    final provider =
        AndroidContactProvider(AndroidContactProviderType.mmsParticipants);

    List<ConversationContactsCompanion> foundConversationContacts = [];
    Map<ConversationsCompanion, List<ContactsCompanion>> conversationContacts =
        {};

    for (final ConversationsCompanion _conversation in conversations) {
      final finalConversation =
          (await db.conversationsDao.upsert(_conversation)).toCompanion(false);
      conversationContacts[finalConversation] = [];

      List<MessagesCompanion> conversationMessages = messages
          .where((m) => m.conversationId == finalConversation.id)
          .toList();
      Map<String, ContactsCompanion> messagePeeps = {};

      for (final message in conversationMessages) {
        // Iterate through each message and get the recipients
        final recipients = await provider.queryRaw(message.externalId.value);

        // Match the sender to the conversation
        final senderRaw = recipients.firstWhereOrNull((r) => r.type == '137');
        final ContactsCompanion? sender;
        if (senderRaw != null) {
          ContactsCompanion tempSender = provider.toDriftCompanion(senderRaw);
          final personId = people
              .firstWhereOrNull(
                  (p) => p.externalId.value == senderRaw.externalParentId)
              ?.id;
          tempSender = tempSender.copyWith(personId: personId);
          sender = (await db.contactsDao.upsert(tempSender)).toCompanion(false);
        } else {
          sender = null;
        }

        // Update the message with the sender
        await db.messagesDao.upsert(
          message.copyWith(senderId: Value(sender?.id.value)),
        );

        // Map the recipients to the conversation as a set by externalId
        for (final recipient in recipients) {
          // Skip if this recipient matches the device owner's phone number
          final normalizedRecipientPhone =
              _normalizePhoneNumber(recipient.value);
          if (devicePhoneNumbers.contains(normalizedRecipientPhone)) {
            // print('Skipping device owner as recipient: ${recipient.value}');
            continue;
          }

          messagePeeps.putIfAbsent(
            recipient.value,
            () => provider.toDriftCompanion(recipient),
          );
        }
      }

      // Convert all the message peeps to conversation contacts
      for (final peep in messagePeeps.values) {
        final contact = await db.contactsDao.upsert(peep);

        foundConversationContacts.add(ConversationContactsCompanion(
          createdAt: Value(DateTime.now()),
          modifiedAt: Value(DateTime.now()),
          conversationId: finalConversation.id,
          contactId: Value(contact.id),
        ));
      }
    }

    // Now load the conversation contacts into the database
    List<ConversationContactsCompanion> updatedConversationContacts = [];
    for (final conversationContact in foundConversationContacts) {
      overrideSync == true || sync == true
          ? updatedConversationContacts.add(
              (await db.conversationContactsDao.upsert(conversationContact))
                  .toCompanion(false))
          : updatedConversationContacts.add(conversationContact);
    }

    return updatedConversationContacts;
  }

  // Comprehensive method to get all device owner's phone numbers from multiple sources
  Future<Set<String>> _getDeviceOwnerPhoneNumbers() async {
    final Set<String> phoneNumbers = {};

    try {
      // Get phone numbers from device info
      final deviceInfo = await CuriousPigeon().getDeviceInfo();

      // Source 1: Direct phone_number field
      if (deviceInfo.containsKey('phone_number') &&
          deviceInfo['phone_number'] != null) {
        final phoneNumber = deviceInfo['phone_number'].toString().trim();
        if (phoneNumber.isNotEmpty) {
          phoneNumbers.add(_normalizePhoneNumber(phoneNumber));
        }
      }

      // Source 2: Line1Number from TelephonyManager
      if (deviceInfo.containsKey('line1Number') &&
          deviceInfo['line1Number'] != null) {
        final phoneNumber = deviceInfo['line1Number'].toString().trim();
        if (phoneNumber.isNotEmpty) {
          phoneNumbers.add(_normalizePhoneNumber(phoneNumber));
        }
      }

      // Source 3: SIM cards from device info
      if (deviceInfo.containsKey('sims') && deviceInfo['sims'] is List) {
        for (var sim in deviceInfo['sims'] as List) {
          if (sim is Map) {
            // Check different possible field names for phone numbers in SIM data
            final possibleFields = [
              'phoneNumber',
              'number',
              'msisdn',
              'line1Number'
            ];
            for (var field in possibleFields) {
              if (sim.containsKey(field) && sim[field] != null) {
                final phoneNumber = sim[field].toString().trim();
                if (phoneNumber.isNotEmpty) {
                  phoneNumbers.add(_normalizePhoneNumber(phoneNumber));
                  print('Added phone number from SIM card: $phoneNumber');
                }
              }
            }
          }
        }
      }

      // Source 4: Get from SIM card DAO (now with phoneNumber field)
      final simCards = await db.simCards.select().get();
      for (final sim in simCards) {
        // Check phoneNumber field first
        if (sim.phoneNumber != null && sim.phoneNumber!.isNotEmpty) {
          phoneNumbers.add(_normalizePhoneNumber(sim.phoneNumber!));
          print('Added phone number from SimCard DB: ${sim.phoneNumber}');
        }
        // Fallback to serialNumber if needed
        else if (sim.serialNumber.isNotEmpty && sim.serialNumber != 'UNKNOWN') {
          phoneNumbers.add(_normalizePhoneNumber(sim.serialNumber));
        }
      }

      // Source 5: Try to find outbound messages sent by the device
      final outboundMessages = await (db.messages.select()
            ..where((m) => m.isOutbound.equals(true))
            ..limit(50))
          .get();

      for (final message in outboundMessages) {
        if (message.senderId != null && message.senderId!.isNotEmpty) {
          // Try to get the sender contact using a query instead of getSingleOrNull
          final senderQuery = db.contacts.select()
            ..where((c) => c.id.equals(message.senderId!))
            ..limit(1);
          final senders = await senderQuery.get();

          if (senders.isNotEmpty && senders.first.value.isNotEmpty) {
            phoneNumbers.add(_normalizePhoneNumber(senders.first.value));
          }
        }
      }
    } catch (e, stackTrace) {
      print('Error getting device owner phone numbers: $e');
      print(stackTrace.toString());
    }

    return phoneNumbers;
  }

  // Helper method to normalize phone numbers for comparison
  String _normalizePhoneNumber(String phoneNumber) {
    // Remove all non-digit characters
    final digitsOnly = phoneNumber.replaceAll(RegExp(r'\D'), '');

    // If number begins with country code (e.g., +1 for US), remove it
    // This is a simplified approach - for production, consider a more robust phone number library
    if (digitsOnly.length > 10) {
      return digitsOnly.substring(digitsOnly.length - 10);
    }

    return digitsOnly;
  }

  /// Query messages with optional filters
  @override
  Future<List<MessagesCompanion>> allMessages({
    DateTime? since,
    String? threadId,
    String? address,
    bool? overrideSync = false,
  }) async {
    print('Getting all messages');
    final conversations = await allConversations();

    // Get all messages directly
    final allMmsMessages = await AndroidMmsProvider.mms.query();
    final allSmsMessages = await AndroidSmsProvider.sms.query();
    final allMessages = [...allMmsMessages, ...allSmsMessages];

    print("All direct messages: ${allMessages.length}");

    // Link all messages to their conversation id
    List<MessagesCompanion> linkedMessages = [];
    for (var message in allMessages) {
      final conversation = conversations
          .firstWhere((c) => c.externalId == message.externalParentId);

      MessagesCompanion companion =
          message.copyWith(conversationId: conversation.id);
      linkedMessages.add(companion);
    }

    for (MessagesCompanion companion in linkedMessages) {
      overrideSync == true || sync == true
          ? await db.messagesDao.upsert(companion)
          : companion;
    }

    return linkedMessages;
  }

  /// Get all people from the device
  @override
  Future<List<PeopleCompanion>> allPeople([bool overrideSync = false]) async {
    print('Getting all people');
    final people = await AndroidPersonProvider.contactables.query();
    final updatedPeople = <PeopleCompanion>[];
    for (var person in people) {
      overrideSync == true || sync == true
          ? updatedPeople
              .add((await db.peopleDao.upsert(person)).toCompanion(false))
          : updatedPeople.add(person);
    }
    return updatedPeople;
  }

  /// Get all contacts from the device
  @override
  Future<List<ContactsCompanion>> allContacts(
      [bool overrideSync = false]) async {
    print('Getting all contacts');
    final people = await allPeople();
    final provider =
        AndroidContactProvider(AndroidContactProviderType.contactables);
    final contacts = await provider.queryRaw();

    final finalContacts = <ContactsCompanion>[];
    for (var contact in contacts) {
      final person = people
          .firstWhere((p) => p.externalId.value == contact.externalParentId);
      final finalContact = provider.toDriftCompanion(contact).copyWith(
            personId: person.id,
          );

      overrideSync == true || sync == true
          ? finalContacts.add(
              (await db.contactsDao.upsert(finalContact)).toCompanion(false))
          : finalContacts.add(finalContact);
    }

    return finalContacts;
  }

  /// Get all contacts from messages on the device
  @override
  Future<List<ContactsCompanion>> allContactsFromMessages(
      [bool overrideSync = false]) async {
    print('Getting all contacts from messages');
    final messages = await allMessages(overrideSync: true);

    final provider =
        AndroidContactProvider(AndroidContactProviderType.mmsParticipants);

    // Get all contacts from all messages
    final finalContacts = <ContactsCompanion>[];
    int i = 0;
    for (final message in messages) {
      finalContacts.addAll(await provider.query(message.externalId.value));
      i++;
      print("Processed $i messages");
      print("Contacts: ${finalContacts.length}");
    }
    return finalContacts;
  }
}

/// Extension for command operations
abstract interface class AndroidCommands {
  Future<MessageStatus> fetchSendStatus({required String lookupId});
  Future<bool?> removeSmsFromSim(int id, int threadId);
  Future<bool> markMessageAsRead(String messageId);
  Future<MessagesCompanion> sendMessage({
    required String address,
    required String body,
    List<AttachmentsCompanion>? attachments,
  });
}

class _AndroidCommands implements AndroidCommands {
  /// Get the send status of a message
  @override
  Future<MessageStatus> fetchSendStatus({required String lookupId}) async {
    print('Fetching send status');
    final messages = await AndroidMmsProvider.mms.query();
    return messages
        .firstWhere((m) => m.externalId.value == lookupId)
        .status
        .value;
  }

  /// Remove an SMS from a SIM card
  @override
  Future<bool?> removeSmsFromSim(int id, int threadId) async {
    print('Removing SMS from SIM');
    MethodChannel channel = MethodChannel("simplezen.sms.remove.channel");

    Map arguments = {};
    arguments['id'] = id;
    arguments['thread_id'] = threadId;
    bool? finalResult;
    try {
      final bool? result = await channel.invokeMethod('removeSms', arguments);
      finalResult = result;
    } catch (e, s) {
      AppLogger.error(e.toString(), s);
    }

    return finalResult;
  }

  /// Mark a message as read
  @override
  Future<bool> markMessageAsRead(String messageId) async =>
      throw UnimplementedError();

  /// Send a message
  @override
  Future<MessagesCompanion> sendMessage({
    required String address,
    required String body,
    List<AttachmentsCompanion>? attachments,
  }) async {
    print('Sending message');
    final messageId = DateTime.now().millisecondsSinceEpoch.toString();

    final message = AndroidMms(
      externalId: messageId,
      contentLink: '',
      messageType: '1',
      messageBox: '2',
      priority: '0',
      isSeen: '0',
      spamReport: '0',
      trId: '',
      body: body,
      conversationId: '', // Will be generated by the system
      senderId: address,
      simSlot: '', // TODO: Get from device
      sentAt: DateTime.now(),
      receivedAt: DateTime.now(),
      readAt: DateTime.now(),
      failedAt: null,
      isRead: false,
      isOutbound: true, status: MessageStatus.sent, externalParentId: '',
    );

    try {
      AppLogger.debug("Sending: $message");

      // Create a JSON payload that includes message and attachments
      final Map<String, dynamic> payload = message.toJson();

      // Add attachments to payload if provided
      if (attachments != null && attachments.isNotEmpty) {
        payload['attachments'] = attachments
            .map((attachment) => {
                  'path': attachment.path.value,
                  'mimeType': attachment.mimeType.value,
                })
            .toList();

        AppLogger.debug("With attachments: ${payload['attachments']}");
      }

      await _sargentPigeon.sendMessage(payload);
      AppLogger.debug('Sent');

      // Handle successful sends of messages with attachments
      if (attachments != null && attachments.isNotEmpty) {
        // TODO: Implement additional processing for attachments if needed
        AppLogger.debug('Message sent with ${attachments.length} attachments');
      }

      return message.toDriftCompanion();
    } catch (e, stackTrace) {
      AppLogger.error('Failed to send message', stackTrace);
      rethrow;
    }
  }

  /// Send a notification
  Future<bool> sendNotification({required title, required body}) async {
    print('Sending notification');
    return _sargentPigeon.sendNotification();
  }
}

/// Extension for destructive operations
abstract interface class AndroidDestructiveCommands {
  Future<bool> deleteThread(String threadId);
  Future<bool> deleteContact({required String lookupId});
  Future<bool> deleteMessage({required String lookupId});
}

class _AndroidDestructiveCommands implements AndroidDestructiveCommands {
  /// Delete an entire thread
  @override
  Future<bool> deleteThread(String threadId) async {
    print('Deleting thread');
    return throw UnimplementedError();
  }

  /// Delete a single contact
  @override
  Future<bool> deleteContact({required String lookupId}) async {
    print('Deleting contact');
    return await _kamikazeePigeon.deleteContact(lookupId);
  }

  /// Delete a single message
  @override
  Future<bool> deleteMessage({required String lookupId}) async {
    print('Deleting message');
    return await _kamikazeePigeon.deleteMessage(lookupId);
  }
}

/// Extension for permission-related operations
abstract interface class AndroidPermissions {
  Future<Map<String, bool>> requestPermissions({required Intention intent});
  Future<bool> fetchRole({required Intention intent});
  Future<Map<String, bool>> fetchPermissions({required Intention intent});
  Future<bool> fetchFullAuth({required Intention intent});
  Future<bool> requestRole({required Intention intent});
  Future<bool> requestFullAuth({required Intention intent});
}

class _AndroidPermissions implements AndroidPermissions {
  /// Request the necessary permissions for the given intent
  @override
  Future<Map<String, bool>> requestPermissions(
          {required Intention intent}) async =>
      await _sargentPigeon.requestPermissions(intent.permissions);

  /// Check if the app has the necessary role for the given intent
  @override
  Future<bool> fetchRole({required Intention intent}) async {
    print('Fetching role');
    return await _sargentPigeon.checkRole(intent.role ?? '');
  }

  /// Check if the app has the necessary permissions for the given intent
  @override
  Future<Map<String, bool>> fetchPermissions(
      {required Intention intent}) async {
    print('Fetching permissions');
    return await _sargentPigeon.checkPermissions(intent.permissions);
  }

  /// Check if the app has the necessary role and permissions for the given intent
  @override
  Future<bool> fetchFullAuth({required Intention intent}) async {
    print('Fetching full auth');
    final roleAuthorized = await fetchRole(intent: intent);
    final permissionsAuthorized = await fetchPermissions(intent: intent);
    return roleAuthorized &&
        permissionsAuthorized.values.every((element) => element);
  }

  /// Request the necessary role for the given intent
  Future<bool> requestRole({required Intention intent}) async {
    print('Requesting role');
    return await _sargentPigeon.requestRole(intent.role ?? '');
  }

  /// Request the necessary role and permissions for the given intent
  Future<bool> requestFullAuth({required Intention intent}) async {
    print('Requesting full auth');
    final bool roleAuthorized = await requestRole(intent: intent);
    final Map<String, bool> permissionsAuthorized =
        await requestPermissions(intent: intent);

    return roleAuthorized &&
        permissionsAuthorized.values.every((element) => element);
  }
}

/// Handler for incoming SMS/MMS messages
@pragma('vm:entry-point')
class InboundMessaging implements IncomingPigeon {
  @override
  Future<bool> receiveInboundMessage(
      Map<String, dynamic> inboundMessage) async {
    print('Received Message: ${inboundMessage['body']}');

    String? messageId;

    if (inboundMessage["message_type"] == "sms") {
      AndroidSms message = AndroidSms.fromJson(inboundMessage);
      print('Message: $message');

      message.conversationId = (await (db.conversations.select()
                ..where((c) => c.externalId.equals(message.externalParentId)))
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
          'Unknown message type: ${inboundMessage.keys.toList().toString()}');
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

              await db.attachmentsDao.upsert(attachmentCompanion);
              AppLogger.debug(
                  'Processed attachment: $path with type $mimeType');
            }
          }
        }
      }
    }

    return true;
  }
}

// TODO: Link up to Attachments (creating them if they don't exist)

// TODO: Check for SMS permissions and default SMS app

// TODO: Also check for multi-sim conundrums
// > https://developer.android.com/reference/android/telephony/SmsManager#getSubscriptionId()
// > https://developer.android.com/reference/android/telephony/SmsManager#getSmsManagerForSubscriptionId(int)

// TODO: Also check for any barriers, ie SMS capacity on the SIM card
// > https://developer.android.com/reference/android/telephony/SmsManager#getSmsCapacityOnIcc()

// TODO: https://docs.flutter.dev/platform-integration/android/restore-state-andr
