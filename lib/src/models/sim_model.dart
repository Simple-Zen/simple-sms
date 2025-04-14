import 'package:simple_sms/src/models/query_obj.dart';


class AndroidSimCardProvider {
  AndroidSimCardProvider();
  SimCardFields get provider => _SimCardFields();
}

class AndroidSimCard implements SimCardProvider {
  @override
  final bool isNetworkRoaming;
  @override
  final int slot;
  @override
  final SimCardState state;
  @override
  final String operatorName;
  @override
  final String countryIso;
  @override
  final String serialNumber;
  @override
  final String? carrierName;
  @override
  final String? mcc; //  Mobile Country Code
  @override
  final String? mnc; //  Mobile Network Code
  @override
  final String? error;
  final String? phoneNumber;
  final String? imei;

  AndroidSimCard({
    required this.isNetworkRoaming,
    required this.slot,
    required this.state,
    required this.operatorName,
    required this.countryIso,
    required this.serialNumber,
    this.carrierName,
    this.mcc,
    this.mnc,
    this.error,
    this.phoneNumber,
    this.imei,
  });

  @override
  SimCardsCompanion toDriftCompanion() {
    return SimCardsCompanion(
      isNetworkRoaming: Value(isNetworkRoaming),
      slot: Value(slot),
      state: Value(state),
      operatorName: Value(operatorName),
      countryIso: Value(countryIso),
      serialNumber: Value(serialNumber),
      carrierName: Value(carrierName),
      mcc: Value(mcc),
      mnc: Value(mnc),
      error: Value(error),
      phoneNumber: Value(phoneNumber),
      imei: Value(imei),
    );
  }

  @override
  factory AndroidSimCard.fromDriftCompanion(SimCardsCompanion companion) =>
      AndroidSimCard(
        isNetworkRoaming: companion.isNetworkRoaming.value,
        slot: companion.slot.value,
        state: companion.state.value,
        operatorName: companion.operatorName.value,
        countryIso: companion.countryIso.value,
        serialNumber: companion.serialNumber.value,
        carrierName: companion.carrierName.value,
        mcc: companion.mcc.value,
        mnc: companion.mnc.value,
        error: companion.error.value,
        phoneNumber: companion.phoneNumber.value,
        imei: companion.imei.value,
      );

  factory AndroidSimCard.fromJson(Map<String, dynamic> json) => AndroidSimCard(
        isNetworkRoaming: json['isNetworkRoaming'] == true ||
            json['isNetworkRoaming'] == 'true',
        slot: int.tryParse(json['slot']?.toString() ?? '0') ?? 0,
        state: _parseSimCardState(json['state']?.toString() ?? 'UNKNOWN'),
        operatorName: json['operatorName']?.toString() ?? '',
        countryIso: json['countryIso']?.toString() ?? '',
        serialNumber: json['serialNumber']?.toString() ?? '',
        carrierName: json['carrierName']?.toString(),
        mcc: json['mcc']?.toString(),
        mnc: json['mnc']?.toString(),
        error: json['error']?.toString(),
        phoneNumber: json['phoneNumber']?.toString(),
        imei: json['imei']?.toString(),
      );

  Map<String, dynamic> toJson() => {
        'isNetworkRoaming': isNetworkRoaming,
        'slot': slot,
        'state': state,
        'operatorName': operatorName,
        'countryIso': countryIso,
        'serialNumber': serialNumber,
        'carrierName': carrierName,
        'mcc': mcc,
        'mnc': mnc,
        'error': error,
        'phoneNumber': phoneNumber,
        'imei': imei,
      };

  // Helper method to parse string to SimCardState enum
  static SimCardState _parseSimCardState(String stateString) {
    switch (stateString.toLowerCase()) {
      case 'absent':
        return SimCardState.absent;
      case 'present':
      case 'ready':
        return SimCardState.ready;
      case 'service_provider_locked':
      case 'network_locked':
      case 'restricted':
        return SimCardState.locked;
      case 'pin_required':
        return SimCardState.needsPin;
      case 'puk_required':
        return SimCardState.needsPuk;
      default:
        return SimCardState.unknown;
    }
  }
}

class _SimCardFields extends SimCardFields {}

abstract interface class SimCardFields {
  String get externalId => 'externalId';
  String get isNetworkRoaming => 'isNetworkRoaming';
  String get slot => 'slot';
  String get state => 'state';
  String get operatorName => 'operatorName';
  String get countryIso => 'countryIso';
  String get serialNumber => 'serialNumber';
  String get carrierName => 'carrierName';
  String get mcc => 'mcc';
  String get mnc => 'mnc';
  String get error => 'error';
  String get phoneNumber => 'phoneNumber';
  String get imei => 'imei';
}

extension AndroidSimCardDriftConversion on AndroidSimCardProvider {
  Future<SimCardsCompanion> toDriftCompanion(AndroidSimCard simCard) async =>
      SimCardsCompanion(
        deviceId: Value((await (db.devices.select()).getSingle()).id),
        isNetworkRoaming: Value(simCard.isNetworkRoaming),
        slot: Value(simCard.slot),
        state: Value(simCard.state),
        operatorName: Value(simCard.operatorName),
        countryIso: Value(simCard.countryIso),
        serialNumber: Value(simCard.serialNumber),
        carrierName: Value(simCard.carrierName),
        mcc: Value(simCard.mcc),
        mnc: Value(simCard.mnc),
        error: Value(simCard.error),
        phoneNumber: Value(simCard.phoneNumber),
        imei: Value(simCard.imei),
      );

  Future<SimCardsCompanion> toDriftFromJson(Map<String, dynamic> json) async {
    return SimCardsCompanion(
      deviceId: Value((await (db.devices.select()).getSingle()).id),
      isNetworkRoaming: Value(json[provider.isNetworkRoaming]),
      slot: Value(json[provider.slot]),
      state: Value(AndroidSimCard._parseSimCardState(
          json[provider.state]?.toString() ?? 'UNKNOWN')),
      operatorName: Value(json[provider.operatorName]?.toString() ?? ''),
      countryIso: Value(json[provider.countryIso]?.toString() ?? ''),
      serialNumber: Value(json[provider.serialNumber]?.toString() ?? ''),
      carrierName: Value(json[provider.carrierName]?.toString()),
      mcc: Value(json[provider.mcc]?.toString()),
      mnc: Value(json[provider.mnc]?.toString()),
      error: Value(json[provider.error]?.toString()),
      phoneNumber: Value(json[provider.phoneNumber]?.toString()),
      imei: Value(json[provider.imei]?.toString()),
    );
  }
}

extension AndroidSimCardJsonConversion on AndroidSimCardProvider {
  Map<String, dynamic> toJson(AndroidSimCard simCard) => {
        provider.isNetworkRoaming: simCard.isNetworkRoaming,
        provider.slot: simCard.slot,
        provider.state: simCard.state,
        provider.operatorName: simCard.operatorName,
        provider.countryIso: simCard.countryIso,
        provider.serialNumber: simCard.serialNumber,
        provider.carrierName: simCard.carrierName,
        provider.mcc: simCard.mcc,
        provider.mnc: simCard.mnc,
        provider.error: simCard.error,
        provider.phoneNumber: simCard.phoneNumber,
        provider.imei: simCard.imei,
      };

  AndroidSimCard fromJson(Map<String, dynamic> json) {
    return AndroidSimCard(
      isNetworkRoaming: json[provider.isNetworkRoaming] == true ||
          json[provider.isNetworkRoaming] == 'true',
      slot: int.tryParse(json[provider.slot]?.toString() ?? '0') ?? 0,
      state: AndroidSimCard._parseSimCardState(
          json[provider.state]?.toString() ?? 'UNKNOWN'),
      operatorName: json[provider.operatorName]?.toString() ?? '',
      countryIso: json[provider.countryIso]?.toString() ?? '',
      serialNumber: json[provider.serialNumber]?.toString() ?? '',
      carrierName: json[provider.carrierName]?.toString() ?? '',
      mcc: json[provider.mcc]?.toString() ?? '',
      mnc: json[provider.mnc]?.toString() ?? '',
      error: json[provider.error]?.toString() ?? '',
      phoneNumber: json[provider.phoneNumber]?.toString() ?? '',
      imei: json[provider.imei]?.toString() ?? '',
    );
  }
}

extension AndroidSimCardQueries on AndroidSimCardProvider {
  Future<List<SimCardsCompanion>> query([String? lookupKey]) async {
    List<SimCardsCompanion> companions = [];

    final sims = await CuriousPigeon().getSimInfo();

    for (final sim in sims) {
      AndroidSimCard androidSimCard = fromJson(Map<String, dynamic>.from(sim));
      companions.add(await toDriftCompanion(androidSimCard));
    }
    return companions;
  }
}
