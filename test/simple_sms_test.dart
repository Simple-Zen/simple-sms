import 'package:flutter_test/flutter_test.dart';
import 'package:simple_sms/simple_sms.dart';
import 'package:simple_sms/simple_sms_platform_interface.dart';
import 'package:simple_sms/simple_sms_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockSimpleSmsPlatform
    with MockPlatformInterfaceMixin
    implements SimpleSmsPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final SimpleSmsPlatform initialPlatform = SimpleSmsPlatform.instance;

  test('$MethodChannelSimpleSms is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelSimpleSms>());
  });

  test('getPlatformVersion', () async {
    SimpleSms simpleSmsPlugin = SimpleSms();
    MockSimpleSmsPlatform fakePlatform = MockSimpleSmsPlatform();
    SimpleSmsPlatform.instance = fakePlatform;

    expect(await simpleSmsPlugin.getPlatformVersion(), '42');
  });
}
