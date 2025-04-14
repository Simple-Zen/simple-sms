import 'package:simple_sms/src/models/query_obj.dart';

enum AndroidContactProviderType {
  contactables,
  mmsParticipants;

  AndroidContactProviderDefinition get definition => switch (this) {
    contactables => _Contactables(),
    mmsParticipants => _MmsParticipants(),
  };
}

class AndroidContactProvider {
  AndroidContactProvider(this.providerType);
  final AndroidContactProviderType providerType;
  AndroidContactProviderDefinition get provider => providerType.definition;
}

class AndroidContact {
  AndroidContact({
    required this.externalId,
    required this.externalParentId,
    required this.personId,
    required this.value,
    required this.sourceMap,
    this.type,
  });

  final String? type;
  final String externalId;
  final String externalParentId;
  final String personId;
  final String value;
  final Map<String, dynamic> sourceMap;
}

abstract interface class AndroidContactProviderDefinition {
  // Content Provider Definitions
  const AndroidContactProviderDefinition();

  String get contentUri;
  List<String> get projection;
  String get selection;
  List<String> get selectionArgs;
  String get sortOrder;

  // Field Name Definitions
  String get externalId;
  String get externalParentId;
  String get phoneNumber;
  String get email;
  String get type;
}

class _MmsParticipants extends AndroidContactProviderDefinition {
  // Android field names
  const _MmsParticipants();

  @override
  String get contentUri => 'content://mms/{lookupKey}/addr';
  @override
  List<String> get projection => [];
  @override
  String get selection => '';
  @override
  List<String> get selectionArgs => [];
  @override
  String get sortOrder => '';

  @override
  String get externalId => '_id';
  @override
  String get externalParentId => 'contact_id';
  @override
  String get phoneNumber => 'address';
  @override
  String get email => '';
  @override
  String get type => 'type';
}

class _Contactables extends AndroidContactProviderDefinition {
  // Android field names
  const _Contactables();

  @override
  String get contentUri => 'content://com.android.contacts/data/contactables';
  @override
  List<String> get projection => [];
  @override
  String get selection => '';
  @override
  List<String> get selectionArgs => [];
  @override
  String get sortOrder => '';

  @override
  String get externalId => '_id';
  @override
  String get externalParentId => 'contact_id';
  @override
  String get phoneNumber => 'data4';
  @override
  String get email => 'data1';
  @override
  String get type => '';
}

extension AndroidContactJsonConversion on AndroidContactProvider {
  Map<String, dynamic> toJson(AndroidContact contact) => {
    provider.externalId: contact.externalId,
    provider.externalParentId: contact.externalParentId,
    provider.phoneNumber: contact.value.contains('@') ? '' : contact.value,
    provider.email: contact.value.contains('@') ? contact.value : '',
    provider.type: contact.type,
  };

  AndroidContact fromJson(Map<String, dynamic> json) {
    final externalId = json[provider.externalId]?.toString() ?? '';
    final externalParentId = json[provider.externalParentId]?.toString() ?? '';
    final phoneNumber = json[provider.phoneNumber];
    final email = json[provider.email];
    final type = json[provider.type];
    final personId = null;
    final sourceMap = json;
    final value = phoneNumber != '' ? phoneNumber : email;

    AndroidContact contact = AndroidContact(
      externalId: externalId,
      externalParentId: externalParentId,
      personId: personId,
      sourceMap: sourceMap,
      value: value,
      type: type.toString(),
    );
    return contact;
  }
}

extension AndroidContactQueries on AndroidContactProvider {
  Future<List<AndroidContact>> query([String? lookupKey]) async {
    List<AndroidContact> companions = [];

      QueryObj query = QueryObj(
      contentUri:
          provider.contentUri.contains('{lookupKey}')
              ? provider.contentUri.replaceAll('{lookupKey}', lookupKey!)
              : provider.contentUri,
      projection: provider.projection,
      selection: provider.selection,
      selectionArgs: provider.selectionArgs,
      sortOrder: provider.sortOrder,
    );

    final contacts = await CuriousPigeon().query(query);

    for (final contact in contacts) {
      AndroidContact androidContact = fromJson(
        Map<String, dynamic>.from(contact),
      );
      companions.add(toDriftCompanion(androidContact));
    }

    return companions;
  }

  Future<List<AndroidContact>> queryRaw([String? lookupKey]) async {
    final contacts = await CuriousPigeon().query(
      QueryObj(
        contentUri:
            provider.contentUri.contains('{lookupKey}')
                ? provider.contentUri.replaceAll('{lookupKey}', lookupKey!)
                : provider.contentUri,
        projection: provider.projection,
        selection: provider.selection,
        selectionArgs: provider.selectionArgs,
        sortOrder: provider.sortOrder,
      ),
    );
    List<AndroidContact> finalContacts = [];
    for (final contact in contacts) {
      AndroidContact androidContact = fromJson(
        Map<String, dynamic>.from(contact),
      );
      finalContacts.add(androidContact);
    }
    return finalContacts;
  }
}
