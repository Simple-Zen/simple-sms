import 'package:simple_sms/src/models/query_obj.dart';
import 'package:flutter/foundation.dart';

class AndroidDeviceProvider {
  AndroidDeviceProvider();
  get provider => _DeviceFields();
}

class AndroidDevice implements DeviceProvider {
  AndroidDevice({
    required this.brand,
    required this.model,
    required this.os,
    required this.externalId,
    required this.externalParentId,
    this.simCards = const [],
  });

  @override
  String brand;

  @override
  String model;

  @override
  String os;

  @override
  String externalId;

  @override
  String externalParentId;

  final List<Map<String, dynamic>> simCards;
}

class _DeviceFields {
  // Android field names
  const _DeviceFields();

  String get externalId => 'external_id';
  String get externalParentId => 'external_parent_id';
  String get brand => 'brand';
  String get model => 'model';
  String get os => 'os';
}

extension AndroidDeviceDriftConversion on AndroidDeviceProvider {
  DevicesCompanion toDriftCompanion(AndroidDevice device) => DevicesCompanion(
    externalId: Value(device.externalId),
    externalParentId: Value(device.externalParentId),
    brand: Value(device.brand),
    model: Value(device.model),
    os: Value(device.os),
  );

  DevicesCompanion toDriftFromJson(Map<String, dynamic> json) {
    return DevicesCompanion(
      externalId: Value(json[provider.externalId]),
      externalParentId: Value(json[provider.externalParentId]),
      brand: Value(json[provider.brand]),
      model: Value(json[provider.model]),
      os: Value(json[provider.os]),
    );
  }
}

extension AndroidDeviceJsonConversion on AndroidDeviceProvider {
  Map<String, dynamic> toJson(AndroidDevice device) => {
    provider.externalId: device.externalId,
    provider.externalParentId: device.externalParentId,
    provider.brand: device.brand,
    provider.model: device.model,
    provider.os: device.os,
    'simCards': device.simCards,
  };

  AndroidDevice fromJson(Map<String, dynamic> json) {
    // Extract sim cards if present
    List<Map<String, dynamic>> simCards = [];
    if (json.containsKey('sims') && json['sims'] is List) {
      simCards =
          (json['sims'] as List)
              .map((sim) => Map<String, dynamic>.from(sim))
              .toList();
    }

    return AndroidDevice(
      externalId: json[provider.externalId]?.toString() ?? '',
      externalParentId: json[provider.externalParentId]?.toString() ?? '',
      brand: json[provider.brand]?.toString() ?? '',
      model: json[provider.model]?.toString() ?? '',
      os: json[provider.os]?.toString() ?? '',
      simCards: simCards,
    );
  }
}

extension SimCardConversion on AndroidDeviceProvider {
  List<SimCardsCompanion> simCardsToDriftCompanions(
    String deviceId,
    List<Map<String, dynamic>> simCardsJson,
  ) {
    return simCardsJson.map((simJson) {
      return SimCardsCompanion(
        deviceId: Value(deviceId),
        slot: Value(
          simJson['slot'] is int
              ? simJson['slot']
              : int.tryParse(simJson['slot']?.toString() ?? '0') ?? -1,
        ),
        isNetworkRoaming: Value(
          simJson['isNetworkRoaming'] == true ||
              simJson['isNetworkRoaming'] == 'true',
        ),
        state: Value(
          parseSimCardState(simJson['state']?.toString() ?? 'UNKNOWN'),
        ),
        operatorName: Value(simJson['operatorName']?.toString() ?? 'UNKNOWN'),
        countryIso: Value(simJson['countryIso']?.toString() ?? 'UNKNOWN'),
        serialNumber: Value(simJson['serialNumber']?.toString() ?? 'UNKNOWN'),
        carrierName: Value(simJson['carrierName']?.toString()),
        mcc: Value(simJson['mcc']?.toString()),
        mnc: Value(simJson['mnc']?.toString()),
        error: Value(simJson['error']?.toString()),
        phoneNumber: Value(simJson['phoneNumber']?.toString()),
        imei: Value(simJson['imei']?.toString()),
      );
    }).toList();
  }

  // Helper method to parse string to SimCardState enum
  SimCardState parseSimCardState(String stateString) {
    switch (stateString.toUpperCase()) {
      case 'ABSENT':
        return SimCardState.absent;
      case 'PRESENT':
      case 'READY':
        return SimCardState.ready;
      case 'SERVICE_PROVIDER_LOCKED':
      case 'NETWORK_LOCKED':
      case 'RESTRICTED':
        return SimCardState.locked;
      case 'PIN_REQUIRED':
        return SimCardState.needsPin;
      case 'PUK_REQUIRED':
        return SimCardState.needsPuk;
      case 'NOT_READY':
      case 'PERM_DISABLED':
      case 'CARD_IO_ERROR':
      default:
        return SimCardState.unknown;
    }
  }
}

extension AndroidDeviceQueries on AndroidDeviceProvider {
  Future<DevicesCompanion> query([String? lookupKey]) async {
    Map<String, Object?> device = {};
    try {
      device = await CuriousPigeon().getDeviceInfo();
      device['external_id'] = await FirebaseInstallations.instance.getId();
    } catch (e) {
      debugPrint('Error getting device info: $e');
      // Provide fallback values for device information
      device = {
        'brand': 'Unknown',
        'model': 'Unknown',
        'os': 'Unknown',
        'external_id': await FirebaseInstallations.instance.getId(),
        'sims': [],
      };
    }

    AndroidDevice androidDevice = fromJson(Map<String, dynamic>.from(device));
    return toDriftCompanion(androidDevice);
  }

  // Add a method to get SIM cards from device info
  Future<List<SimCardsCompanion>> querySimCards() async {
    Map<String, Object?> device = {};
    try {
      device = await CuriousPigeon().getDeviceInfo();
    } catch (e) {
      debugPrint('Error getting device info for SIM cards: $e');
      return []; // Return empty list if we can't get device info
    }

    final deviceId = await FirebaseInstallations.instance.getId();
    AndroidDevice androidDevice = fromJson(Map<String, dynamic>.from(device));
    return simCardsToDriftCompanions(deviceId, androidDevice.simCards);
  }
}
