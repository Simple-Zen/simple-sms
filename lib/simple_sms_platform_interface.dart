import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'simple_sms_method_channel.dart';

abstract class SimpleSmsPlatform extends PlatformInterface {
  /// Constructs a SimpleSmsPlatform.
  SimpleSmsPlatform() : super(token: _token);

  static final Object _token = Object();

  static SimpleSmsPlatform _instance = MethodChannelSimpleSms();

  /// The default instance of [SimpleSmsPlatform] to use.
  ///
  /// Defaults to [MethodChannelSimpleSms].
  static SimpleSmsPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [SimpleSmsPlatform] when
  /// they register themselves.
  static set instance(SimpleSmsPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
