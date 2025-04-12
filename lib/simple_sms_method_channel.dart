import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'simple_sms_platform_interface.dart';

/// An implementation of [SimpleSmsPlatform] that uses method channels.
class MethodChannelSimpleSms extends SimpleSmsPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('simple_sms');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
