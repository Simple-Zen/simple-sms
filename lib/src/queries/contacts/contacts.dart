import 'dart:core';

class ContactsQuery {
  const ContactsQuery({required this.fields});
  static const contentUri = 'content://com.android.contacts/contacts';
  final List<ContactFields> fields;

  String get queryString {
    return fields.map((e) => e.stringValue).join(',');
  }
}

enum ContactFields {
  all(''),
  id('_id'),
  chatCapability('contact_chat_capability'),
  lastUpdatedTimestamp('contact_last_updated_timestamp'),
  presence('contact_presence'),
  status('contact_status'),
  statusIcon('contact_status_icon'),
  statusLabel('contact_status_label'),
  statusResId('contact_status_res_package'),
  statusType('contact_status_ts'),
  statusUri('custom_ringtone'),
  type('dirty_contact'),
  typeResId('display_name'),
  typeString('display_name_alt'),
  displayNameReverse('display_name_reverse'),
  displayNameSource('display_name_source'),
  hasEmail('has_email'),
  hasPhoneNumber('has_phone_number'),
  inDefaultDirectory('in_default_directory'),
  inVisibleGroup('in_visible_group'),
  isPrivate('is_private'),
  isUserProfile('is_user_profile'),
  lastTimeContacted('last_time_contacted'),
  link('link'),
  linkCount('link_count'),
  linkType1('link_type1'),
  lookupKey('lookup'),
  nameRawContactId('name_raw_contact_id'),
  phonebookBucket('phonebook_bucket'),
  phonebookAltBucket('phonebook_bucket_alt'),
  phonebookLabel('phonebook_label'),
  phonebookAltLabel('phonebook_label_alt'),
  phoneticName('phonetic_name'),
  phoneticNameStyle('phonetic_name_style'),
  photoFileId('photo_file_id'),
  photoId('photo_id'),
  photoThumbUri('photo_thumb_uri'),
  photoUri('photo_uri'),
  isPinned('pinned'),
  secCallBackground('sec_call_background'),
  secCustmoAlert('sec_custom_alert'),
  secCustomVibration('sec_custom_vibration'),
  secLed('sec_led'),
  secPreferredSim('sec_preferred_sim'),
  secPreferredVideoCallAccountId('sec_preferred_video_call_account_id'),
  secPreferredVideoCallAccountName('sec_preferred_video_call_account_name'),
  sendToVoicemail('send_to_voicemail'),
  sortKey('sort_key'),
  sortKeyAlt('sort_key_alt'),
  isStarred('starred'),
  timesContacted('times_contacted');

  const ContactFields(this.stringValue);
  final String stringValue;
}
