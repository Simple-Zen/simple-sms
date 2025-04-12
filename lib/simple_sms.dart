
import 'simple_sms_platform_interface.dart';

class SimpleSms {
  Future<String?> getPlatformVersion() {
    return SimpleSmsPlatform.instance.getPlatformVersion();
  }
}
