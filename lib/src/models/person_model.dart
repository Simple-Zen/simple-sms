import 'package:simple_sms/src/models/query_obj.dart';


enum AndroidPersonProvider implements EnumProvider {
  contactables(
    contentUri: 'content://com.android.contacts/data/contactables',
    projection: [
      AndroidPersonFields.accountName,
      AndroidPersonFields.externalId,
      AndroidPersonFields.accountType,
      AndroidPersonFields.createdAt,
      AndroidPersonFields.phoneticName,
      AndroidPersonFields.ringtone,
      AndroidPersonFields.photoUri,
      AndroidPersonFields.photoThumbnailUri,
      AndroidPersonFields.starred,
      AndroidPersonFields.pinned,
      AndroidPersonFields.sendToVoicemail,
      AndroidPersonFields.displayName,
    ],
  ),
  ;

  const AndroidPersonProvider({
    required this.contentUri,
    required this.projection,
  });
  final String contentUri;
  final List<String> projection;

  @override
  Future<List<PeopleCompanion>> query() async {
    List<PeopleCompanion> companions = [];
    final people = await CuriousPigeon().query(QueryObj(
      contentUri: contentUri,
      sortOrder: 'display_name ASC',
      projection: projection,
    ));

    for (final person in people) {
      AndroidPerson androidPerson =
          AndroidPerson.fromJson(Map<String, dynamic>.from(person));
      companions.add(androidPerson.toDriftCompanion());
    }

    return companions;
  }
}

class AndroidPersonFields {
  // Android field names
  static const accountName = 'account_name';
  static const accountType = 'account_type';
  static const externalId = 'contact_id';
  static const phoneNumber = 'data4';
  static const email = 'data1';
  static const createdAt = 'creation_time';
  static const modifiedAt = 'contact_last_updated_timestamp';
  static const phoneticName = 'phonetic_name';
  static const ringtone = 'custom_ringtone';
  static const photoUri = 'photo_uri';
  static const photoThumbnailUri = 'photo_thumb_uri';
  static const starred = 'starred';
  static const pinned = 'pinned';
  static const sendToVoicemail = 'send_to_voicemail';
  static const displayName = 'display_name_reverse';
}

class AndroidPerson implements PersonProvider {
  @override
  final String? id;
  @override
  final String? externalId;
  @override
  final String accountName;
  @override
  final String accountType;
  @override
  final String? emailValue;
  @override
  final String? phoneValue;
  @override
  final String firstName;
  @override
  final String lastName;
  @override
  final String? ringtone;
  @override
  final String? phoneticName;
  @override
  final String? photoUri;
  @override
  final String? photoThumbnailUri;
  @override
  final bool? starred;
  @override
  final bool? pinned;
  @override
  final bool? sendToVoicemail;

  AndroidPerson({
    required this.accountName,
    required this.accountType,
    required this.firstName,
    required this.lastName,
    this.id,
    this.externalId,
    this.emailValue,
    this.phoneValue,
    this.ringtone,
    this.phoneticName,
    this.photoUri,
    this.photoThumbnailUri,
    this.starred,
    this.pinned,
    this.sendToVoicemail,
  });

  @override
  PeopleCompanion toDriftCompanion() {
    return PeopleCompanion(
      externalId: Value(externalId ?? ''),
      accountName: Value(accountName),
      accountType: Value(accountType),
      firstName: Value(firstName),
      lastName: Value(lastName),
      ringtone: Value(ringtone),
      phoneticName: Value(phoneticName),
      photoUri: Value(photoUri),
      photoThumbnailUri: Value(photoThumbnailUri),
      starred: Value(starred),
    );
  }

  factory AndroidPerson.fromDriftCompanion(PeopleCompanion companion) {
    return AndroidPerson(
      externalId: companion.externalId.value,
      accountName: companion.accountName.value,
      accountType: companion.accountType.value,
      firstName: companion.firstName.value,
      lastName: companion.lastName.value,
      ringtone: companion.ringtone.value,
      phoneticName: companion.phoneticName.value,
      photoUri: companion.photoUri.value,
      photoThumbnailUri: companion.photoThumbnailUri.value,
      starred: companion.starred.value,
    );
  }

  factory AndroidPerson.fromJson(Map<String, dynamic> json) => AndroidPerson(
        externalId: json[AndroidPersonFields.externalId].toString(),
        accountName: json[AndroidPersonFields.accountName] ?? '',
        accountType: json[AndroidPersonFields.accountType] ?? '',
        emailValue: json[AndroidPersonFields.email] ?? '',
        phoneValue: json[AndroidPersonFields.phoneNumber] ?? '',
        firstName: json[AndroidPersonFields.displayName].split(',').last,
        lastName: json[AndroidPersonFields.displayName].split(',').first,
        ringtone: json[AndroidPersonFields.ringtone],
        phoneticName: json[AndroidPersonFields.phoneticName],
        photoUri: json[AndroidPersonFields.photoUri],
        photoThumbnailUri: json[AndroidPersonFields.photoThumbnailUri],
        starred: json[AndroidPersonFields.starred] == 1,
        pinned: json[AndroidPersonFields.pinned] == 1,
        sendToVoicemail: json[AndroidPersonFields.sendToVoicemail] == 1,
      );

  Map<String, dynamic> toJson() => {
        AndroidPersonFields.externalId: externalId,
        AndroidPersonFields.accountName: accountName,
        AndroidPersonFields.accountType: accountType,
        AndroidPersonFields.email: emailValue,
        AndroidPersonFields.phoneNumber: phoneValue,
        AndroidPersonFields.displayName: '$lastName, $firstName',
        AndroidPersonFields.ringtone: ringtone,
        AndroidPersonFields.phoneticName: phoneticName,
        AndroidPersonFields.photoUri: photoUri,
        AndroidPersonFields.photoThumbnailUri: photoThumbnailUri,
        AndroidPersonFields.starred: starred,
        AndroidPersonFields.pinned: pinned,
        AndroidPersonFields.sendToVoicemail: sendToVoicemail,
      };
}
