package io.simplezen.simple_sms

import android.app.role.RoleManager
import android.content.Context
import android.content.Context.ROLE_SERVICE
import android.content.Intent
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler

/** SimpleSmsPlugin */
class MainActivity: FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel

  companion object {
    lateinit var roleManager: RoleManager
    // Store reference to the FlutterEngine for use from InboundMessaging
    private var flutterEngine: FlutterEngine? = null

    private lateinit var requestRoleLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>
    val permissionResults = mutableMapOf<String, Boolean>()

    // Method to access FlutterEngine from other classes
    fun getFlutterEngine(): FlutterEngine? {
      return flutterEngine
    }

    fun checkRole(context: Context, role: String): Boolean {
      val roleManager = context.getSystemService(ROLE_SERVICE) as RoleManager
      return roleManager.isRoleAvailable(role) && roleManager.isRoleHeld(role)
    }

    fun checkPermissions(context: Context, permissions: Array<String>): Map<String, Boolean> {
      val permissionResults = mutableMapOf<String, Boolean>()
      for (permission in permissions) {
        val permissionGranted = context.checkSelfPermission(permission)
        permissionResults[permission] = permissionGranted == 0
      }
      return permissionResults
    }

    fun requestPermissions(permissions: Array<String>): Map<String, Boolean> {
      // var grantedCheck: Boolean = false

      if (permissions.isEmpty()) {
        return mapOf()
      }

      // prepareIntentLauncher()
      permissionsLauncher.launch(permissions)
      return permissionResults
    }

    fun requestRole(role: String): Boolean {

      // Check if Role is available.
      val isRoleAvailable = roleManager.isRoleAvailable(role)
      if (!isRoleAvailable) {
        throw Exception("Invalid Role - $role")
      }

      // Check if Role already held.
      var isRoleHeld = roleManager.isRoleHeld(role)
      if (isRoleHeld) {
        return true
      }

      val roleRequestIntent: Intent = roleManager.createRequestRoleIntent(role)
      requestRoleLauncher.launch(roleRequestIntent)

      // Return the current state of the role (this might not reflect the final
      // result)
      return roleManager.isRoleHeld(role)
    }
  }
  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "simple_sms")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
    if (call.method == "getPlatformVersion") {
      result.success("Android ${Build.VERSION.RELEASE}")
    } else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
}

private class MainActivityPriv