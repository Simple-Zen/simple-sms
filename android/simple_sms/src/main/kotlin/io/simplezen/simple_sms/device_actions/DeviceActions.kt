package io.simplezen.simple_sms.src.device_actions

// SargentPigeon
class DeviceActions(val context: Context) : SargentPigeon {
    override fun getSendStatus(messageId: String): String {
        throw Exception("Not implemented")
    }

    override fun checkPermissions(permissions: List<String>): Map<String, Boolean> {
        return MainActivity.Companion.checkPermissions(context, permissions.toTypedArray())
    }

    override fun requestPermissions(permissions: List<String>): Map<String, Boolean> {
        return MainActivity.Companion.requestPermissions(permissions.toTypedArray())
    }

    override fun sendNotification(): Boolean {
        TODO("Not yet implemented")
    }

    override fun checkRole(role: String): Boolean {
        return if (role.isEmpty()) {
            true
        } else {
            MainActivity.Companion.checkRole(context, role)
        }
    }

    override fun requestRole(role: String): Boolean {
        return if (role.isEmpty()) {
            true
        } else {
            MainActivity.Companion.requestRole(role)
        }
    }

    override fun sendMessage(message: Map<String, Any?>): String {
        return OutboundMessagingHandler().sendSms(context, message)
    }

    // New method to load MMS attachment content
    override fun loadMmsAttachment(contentUri: String): BinaryData? {
        try {
            val uri = contentUri.toUri()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val outputStream = ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                Log.d("DeviceActions", "Successfully loaded MMS attachment: $contentUri")
                return BinaryData(outputStream.toByteArray().map { it.toLong() })
            }
        } catch (e: Exception) {
            Log.e("DeviceActions", "Error loading MMS attachment: ${e.message}")
            e.printStackTrace()
        }
        return null
    }

    // New method to save MMS attachment to a temporary file
    override fun saveMmsAttachmentToFile(contentUri: String): String? {
        try {
            val uri = contentUri.toUri()
            val fileName = "mms_${System.currentTimeMillis()}"

            // Get MIME type to determine file extension
            var mimeType = "application/octet-stream"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val mimeTypeIndex = cursor.getColumnIndex(Mms.Part.CONTENT_TYPE)
                    if (mimeTypeIndex != -1) {
                        mimeType = cursor.getString(mimeTypeIndex) ?: mimeType
                    }
                }
            }

            // Determine file extension based on MIME type
            val extension = when {
                mimeType.startsWith("image/") -> ".jpg"
                mimeType.startsWith("video/") -> ".mp4"
                mimeType.startsWith("audio/") -> ".mp3"
                else -> ".bin"
            }

            // Create temp file
            val tempFile = File.createTempFile(fileName, extension, context.cacheDir)

            // Copy content to temp file
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(1024)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                }
            }

            Log.d("DeviceActions", "Saved MMS attachment to: ${tempFile.absolutePath}")
            return tempFile.absolutePath
        } catch (e: Exception) {
            Log.e("DeviceActions", "Error saving MMS attachment: ${e.message}")
            e.printStackTrace()
        }
        return null
    }
}