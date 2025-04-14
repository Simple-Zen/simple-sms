package com.simplezen.unify_messages_plus.src.plugins

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import io.flutter.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import java.io.File
import java.io.FileOutputStream

/**
 * Native implementation of file picker to replace file_picker plugin
 */
class FilePickerPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
    private lateinit var channel: MethodChannel
    private var activity: Activity? = null
    private var pendingResult: MethodChannel.Result? = null
    private val tag = "FilePickerPlugin"

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "simplezen.file_picker.channel")
        channel.setMethodCallHandler(this)
        Log.d(tag, "FilePickerPlugin attached to engine")
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "pickFiles" -> {
                if (activity == null) {
                    result.error("NO_ACTIVITY", "No activity available", null)
                    return
                }

                pendingResult = result
                startFilePicker()
            }
            else -> result.notImplemented()
        }
    }

    private fun startFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        try {
            activity?.startActivityForResult(intent, REQUEST_CODE_PICK_FILES)
        } catch (e: Exception) {
            Log.e(tag, "Error starting file picker: ${e.message}")
            pendingResult?.error("ERROR", "Could not launch file picker: ${e.message}", null)
            pendingResult = null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == REQUEST_CODE_PICK_FILES) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val selectedFiles = mutableListOf<String>()

                // Handle multiple files
                if (data.clipData != null) {
                    val clipData = data.clipData!!
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        val filePath = copyUriToFile(uri)
                        if (filePath != null) {
                            selectedFiles.add(filePath)
                        }
                    }
                }
                // Handle single file
                else if (data.data != null) {
                    val uri = data.data!!
                    val filePath = copyUriToFile(uri)
                    if (filePath != null) {
                        selectedFiles.add(filePath)
                    }
                }

                pendingResult?.success(selectedFiles)
            } else {
                pendingResult?.success(null)
            }

            pendingResult = null
            return true
        }
        return false
    }

    /**
     * Copy content from a URI to a temporary file that Flutter can access
     */
    private fun copyUriToFile(uri: Uri): String? {
        val context = activity?.applicationContext ?: return null

        try {
            // Create a temp file with a unique name
            val fileName = getFileName(uri) ?: "file_${System.currentTimeMillis()}"
            val fileExtension = getFileExtension(fileName)
            val tempFile = File.createTempFile(
                "file_picker_",
                fileExtension,
                context.cacheDir
            )

            // Copy the content from the URI to the temp file
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            Log.d(tag, "Created temp file: ${tempFile.absolutePath}")
            return tempFile.absolutePath
        } catch (e: Exception) {
            Log.e(tag, "Error copying URI to file: ${e.message}")
            return null
        }
    }

    private fun getFileName(uri: Uri): String? {
        val context = activity?.applicationContext ?: return null

        // Try to get the display name from the content resolver
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex("_display_name")
                if (nameIndex != -1) {
                    return cursor.getString(nameIndex)
                }
            }
        }

        // Fallback to getting the last path segment
        return uri.lastPathSegment
    }

    private fun getFileExtension(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf('.')
        return if (lastDotIndex != -1) {
            fileName.substring(lastDotIndex)
        } else {
            // Default extension if none exists
            ".tmp"
        }
    }

    companion object {
        private const val REQUEST_CODE_PICK_FILES = 43289
    }
}