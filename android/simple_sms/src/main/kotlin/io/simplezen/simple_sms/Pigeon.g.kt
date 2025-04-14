// Autogenerated from Pigeon (v24.2.2), do not edit directly.
// See also: https://pub.dev/packages/pigeon
@file:Suppress("UNCHECKED_CAST", "ArrayInDataClass")

package io.simplezen.simple_sms

import android.util.Log
import io.flutter.plugin.common.BasicMessageChannel
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MessageCodec
import io.flutter.plugin.common.StandardMessageCodec
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

private fun wrapResult(result: Any?): List<Any?> {
  return listOf(result)
}

private fun wrapError(exception: Throwable): List<Any?> {
  return if (exception is FlutterError) {
    listOf(
      exception.code,
      exception.message,
      exception.details
    )
  } else {
    listOf(
      exception.javaClass.simpleName,
      exception.toString(),
      "Cause: " + exception.cause + ", Stacktrace: " + Log.getStackTraceString(exception)
    )
  }
}

private fun createConnectionError(channelName: String): FlutterError {
  return FlutterError("channel-error",  "Unable to establish connection on channel: '$channelName'.", "")}

/**
 * Error class for passing custom error details to Flutter via a thrown PlatformException.
 * @property code The error code.
 * @property message The error message.
 * @property details The error details. Must be a datatype supported by the api codec.
 */
class FlutterError (
  val code: String,
  override val message: String? = null,
  val details: Any? = null
) : Throwable()

data class BinaryData (
  val data: List<Long>
)
 {
  companion object {
    fun fromList(pigeonVarList: List<Any?>): BinaryData {
      val data = pigeonVarList[0] as List<Long>
      return BinaryData(data)
    }
  }
  fun toList(): List<Any?> {
    return listOf(
      data,
    )
  }
}

data class QueryObj (
  val contentUri: String,
  val projection: List<String>? = null,
  val selection: String? = null,
  val selectionArgs: List<String>? = null,
  val sortOrder: String? = null
)
 {
  companion object {
    fun fromList(pigeonVarList: List<Any?>): QueryObj {
      val contentUri = pigeonVarList[0] as String
      val projection = pigeonVarList[1] as List<String>?
      val selection = pigeonVarList[2] as String?
      val selectionArgs = pigeonVarList[3] as List<String>?
      val sortOrder = pigeonVarList[4] as String?
      return QueryObj(contentUri, projection, selection, selectionArgs, sortOrder)
    }
  }
  fun toList(): List<Any?> {
    return listOf(
      contentUri,
      projection,
      selection,
      selectionArgs,
      sortOrder,
    )
  }
}

private open class PigeonPigeonCodec : StandardMessageCodec() {
  override fun readValueOfType(type: Byte, buffer: ByteBuffer): Any? {
    return when (type) {
      129.toByte() -> {
        return (readValue(buffer) as? List<Any?>)?.let {
          BinaryData.fromList(it)
        }
      }
      130.toByte() -> {
        return (readValue(buffer) as? List<Any?>)?.let {
          QueryObj.fromList(it)
        }
      }
      else -> super.readValueOfType(type, buffer)
    }
  }
  override fun writeValue(stream: ByteArrayOutputStream, value: Any?)   {
    when (value) {
      is BinaryData -> {
        stream.write(129)
        writeValue(stream, value.toList())
      }
      is QueryObj -> {
        stream.write(130)
        writeValue(stream, value.toList())
      }
      else -> super.writeValue(stream, value)
    }
  }
}

/** Generated class from Pigeon that represents Flutter messages that can be called from Kotlin. */
class IncomingPigeon(private val binaryMessenger: BinaryMessenger, private val messageChannelSuffix: String = "") {
  companion object {
    /** The codec used by IncomingPigeon. */
    val codec: MessageCodec<Any?> by lazy {
      PigeonPigeonCodec()
    }
  }
  fun receiveInboundMessage(inboundMessageArg: Map<String, Any?>, callback: (Result<Boolean>) -> Unit)
{
    val separatedMessageChannelSuffix = if (messageChannelSuffix.isNotEmpty()) ".$messageChannelSuffix" else ""
    val channelName = "dev.flutter.pigeon.unify_messages_plus.IncomingPigeon.receiveInboundMessage$separatedMessageChannelSuffix"
    val channel = BasicMessageChannel<Any?>(binaryMessenger, channelName, codec)
    channel.send(listOf(inboundMessageArg)) {
      if (it is List<*>) {
        if (it.size > 1) {
          callback(Result.failure(FlutterError(it[0] as String, it[1] as String, it[2] as String?)))
        } else if (it[0] == null) {
          callback(Result.failure(FlutterError("null-error", "Flutter api returned null value for non-null return value.", "")))
        } else {
          val output = it[0] as Boolean
          callback(Result.success(output))
        }
      } else {
        callback(Result.failure(createConnectionError(channelName)))
      } 
    }
  }
}
/** Generated interface from Pigeon that represents a handler of messages from Flutter. */
interface CuriousPigeon {
  fun query(query: QueryObj): List<Map<Any?, Any?>>
  fun getDeviceInfo(): Map<String, Any?>
  fun getSimInfo(): List<Map<Any?, Any?>>

  companion object {
    /** The codec used by CuriousPigeon. */
    val codec: MessageCodec<Any?> by lazy {
      PigeonPigeonCodec()
    }
    /** Sets up an instance of `CuriousPigeon` to handle messages through the `binaryMessenger`. */
    @JvmOverloads
    fun setUp(binaryMessenger: BinaryMessenger, api: CuriousPigeon?, messageChannelSuffix: String = "") {
      val separatedMessageChannelSuffix = if (messageChannelSuffix.isNotEmpty()) ".$messageChannelSuffix" else ""
      run {
        val channel = BasicMessageChannel<Any?>(binaryMessenger, "dev.flutter.pigeon.unify_messages_plus.CuriousPigeon.query$separatedMessageChannelSuffix", codec)
        if (api != null) {
          channel.setMessageHandler { message, reply ->
            val args = message as List<Any?>
            val queryArg = args[0] as QueryObj
            val wrapped: List<Any?> = try {
              listOf(api.query(queryArg))
            } catch (exception: Throwable) {
              wrapError(exception)
            }
            reply.reply(wrapped)
          }
        } else {
          channel.setMessageHandler(null)
        }
      }
      run {
        val channel = BasicMessageChannel<Any?>(binaryMessenger, "dev.flutter.pigeon.unify_messages_plus.CuriousPigeon.getDeviceInfo$separatedMessageChannelSuffix", codec)
        if (api != null) {
          channel.setMessageHandler { _, reply ->
            val wrapped: List<Any?> = try {
              listOf(api.getDeviceInfo())
            } catch (exception: Throwable) {
              wrapError(exception)
            }
            reply.reply(wrapped)
          }
        } else {
          channel.setMessageHandler(null)
        }
      }
      run {
        val channel = BasicMessageChannel<Any?>(binaryMessenger, "dev.flutter.pigeon.unify_messages_plus.CuriousPigeon.getSimInfo$separatedMessageChannelSuffix", codec)
        if (api != null) {
          channel.setMessageHandler { _, reply ->
            val wrapped: List<Any?> = try {
              listOf(api.getSimInfo())
            } catch (exception: Throwable) {
              wrapError(exception)
            }
            reply.reply(wrapped)
          }
        } else {
          channel.setMessageHandler(null)
        }
      }
    }
  }
}
/** Generated interface from Pigeon that represents a handler of messages from Flutter. */
interface SargentPigeon {
  fun sendNotification(): Boolean
  fun checkRole(role: String): Boolean
  fun requestRole(role: String): Boolean
  fun sendMessage(message: Map<String, Any?>): String
  fun getSendStatus(messageId: String): String
  fun checkPermissions(permissions: List<String>): Map<String, Boolean>
  fun requestPermissions(permissions: List<String>): Map<String, Boolean>
  fun loadMmsAttachment(contentUri: String): BinaryData?
  fun saveMmsAttachmentToFile(contentUri: String): String?

  companion object {
    /** The codec used by SargentPigeon. */
    val codec: MessageCodec<Any?> by lazy {
      PigeonPigeonCodec()
    }
    /** Sets up an instance of `SargentPigeon` to handle messages through the `binaryMessenger`. */
    @JvmOverloads
    fun setUp(binaryMessenger: BinaryMessenger, api: SargentPigeon?, messageChannelSuffix: String = "") {
      val separatedMessageChannelSuffix = if (messageChannelSuffix.isNotEmpty()) ".$messageChannelSuffix" else ""
      run {
        val channel = BasicMessageChannel<Any?>(binaryMessenger, "dev.flutter.pigeon.unify_messages_plus.SargentPigeon.sendNotification$separatedMessageChannelSuffix", codec)
        if (api != null) {
          channel.setMessageHandler { _, reply ->
            val wrapped: List<Any?> = try {
              listOf(api.sendNotification())
            } catch (exception: Throwable) {
              wrapError(exception)
            }
            reply.reply(wrapped)
          }
        } else {
          channel.setMessageHandler(null)
        }
      }
      run {
        val channel = BasicMessageChannel<Any?>(binaryMessenger, "dev.flutter.pigeon.unify_messages_plus.SargentPigeon.checkRole$separatedMessageChannelSuffix", codec)
        if (api != null) {
          channel.setMessageHandler { message, reply ->
            val args = message as List<Any?>
            val roleArg = args[0] as String
            val wrapped: List<Any?> = try {
              listOf(api.checkRole(roleArg))
            } catch (exception: Throwable) {
              wrapError(exception)
            }
            reply.reply(wrapped)
          }
        } else {
          channel.setMessageHandler(null)
        }
      }
      run {
        val channel = BasicMessageChannel<Any?>(binaryMessenger, "dev.flutter.pigeon.unify_messages_plus.SargentPigeon.requestRole$separatedMessageChannelSuffix", codec)
        if (api != null) {
          channel.setMessageHandler { message, reply ->
            val args = message as List<Any?>
            val roleArg = args[0] as String
            val wrapped: List<Any?> = try {
              listOf(api.requestRole(roleArg))
            } catch (exception: Throwable) {
              wrapError(exception)
            }
            reply.reply(wrapped)
          }
        } else {
          channel.setMessageHandler(null)
        }
      }
      run {
        val channel = BasicMessageChannel<Any?>(binaryMessenger, "dev.flutter.pigeon.unify_messages_plus.SargentPigeon.sendMessage$separatedMessageChannelSuffix", codec)
        if (api != null) {
          channel.setMessageHandler { message, reply ->
            val args = message as List<Any?>
            val messageArg = args[0] as Map<String, Any?>
            val wrapped: List<Any?> = try {
              listOf(api.sendMessage(messageArg))
            } catch (exception: Throwable) {
              wrapError(exception)
            }
            reply.reply(wrapped)
          }
        } else {
          channel.setMessageHandler(null)
        }
      }
      run {
        val channel = BasicMessageChannel<Any?>(binaryMessenger, "dev.flutter.pigeon.unify_messages_plus.SargentPigeon.getSendStatus$separatedMessageChannelSuffix", codec)
        if (api != null) {
          channel.setMessageHandler { message, reply ->
            val args = message as List<Any?>
            val messageIdArg = args[0] as String
            val wrapped: List<Any?> = try {
              listOf(api.getSendStatus(messageIdArg))
            } catch (exception: Throwable) {
              wrapError(exception)
            }
            reply.reply(wrapped)
          }
        } else {
          channel.setMessageHandler(null)
        }
      }
      run {
        val channel = BasicMessageChannel<Any?>(binaryMessenger, "dev.flutter.pigeon.unify_messages_plus.SargentPigeon.checkPermissions$separatedMessageChannelSuffix", codec)
        if (api != null) {
          channel.setMessageHandler { message, reply ->
            val args = message as List<Any?>
            val permissionsArg = args[0] as List<String>
            val wrapped: List<Any?> = try {
              listOf(api.checkPermissions(permissionsArg))
            } catch (exception: Throwable) {
              wrapError(exception)
            }
            reply.reply(wrapped)
          }
        } else {
          channel.setMessageHandler(null)
        }
      }
      run {
        val channel = BasicMessageChannel<Any?>(binaryMessenger, "dev.flutter.pigeon.unify_messages_plus.SargentPigeon.requestPermissions$separatedMessageChannelSuffix", codec)
        if (api != null) {
          channel.setMessageHandler { message, reply ->
            val args = message as List<Any?>
            val permissionsArg = args[0] as List<String>
            val wrapped: List<Any?> = try {
              listOf(api.requestPermissions(permissionsArg))
            } catch (exception: Throwable) {
              wrapError(exception)
            }
            reply.reply(wrapped)
          }
        } else {
          channel.setMessageHandler(null)
        }
      }
      run {
        val channel = BasicMessageChannel<Any?>(binaryMessenger, "dev.flutter.pigeon.unify_messages_plus.SargentPigeon.loadMmsAttachment$separatedMessageChannelSuffix", codec)
        if (api != null) {
          channel.setMessageHandler { message, reply ->
            val args = message as List<Any?>
            val contentUriArg = args[0] as String
            val wrapped: List<Any?> = try {
              listOf(api.loadMmsAttachment(contentUriArg))
            } catch (exception: Throwable) {
              wrapError(exception)
            }
            reply.reply(wrapped)
          }
        } else {
          channel.setMessageHandler(null)
        }
      }
      run {
        val channel = BasicMessageChannel<Any?>(binaryMessenger, "dev.flutter.pigeon.unify_messages_plus.SargentPigeon.saveMmsAttachmentToFile$separatedMessageChannelSuffix", codec)
        if (api != null) {
          channel.setMessageHandler { message, reply ->
            val args = message as List<Any?>
            val contentUriArg = args[0] as String
            val wrapped: List<Any?> = try {
              listOf(api.saveMmsAttachmentToFile(contentUriArg))
            } catch (exception: Throwable) {
              wrapError(exception)
            }
            reply.reply(wrapped)
          }
        } else {
          channel.setMessageHandler(null)
        }
      }
    }
  }
}
/** Generated interface from Pigeon that represents a handler of messages from Flutter. */
interface KamikazeePigeon {
  fun deleteMessage(lookupId: String): Boolean
  fun deleteContact(lookupId: String): Boolean

  companion object {
    /** The codec used by KamikazeePigeon. */
    val codec: MessageCodec<Any?> by lazy {
      PigeonPigeonCodec()
    }
    /** Sets up an instance of `KamikazeePigeon` to handle messages through the `binaryMessenger`. */
    @JvmOverloads
    fun setUp(binaryMessenger: BinaryMessenger, api: KamikazeePigeon?, messageChannelSuffix: String = "") {
      val separatedMessageChannelSuffix = if (messageChannelSuffix.isNotEmpty()) ".$messageChannelSuffix" else ""
      run {
        val channel = BasicMessageChannel<Any?>(binaryMessenger, "dev.flutter.pigeon.unify_messages_plus.KamikazeePigeon.deleteMessage$separatedMessageChannelSuffix", codec)
        if (api != null) {
          channel.setMessageHandler { message, reply ->
            val args = message as List<Any?>
            val lookupIdArg = args[0] as String
            val wrapped: List<Any?> = try {
              listOf(api.deleteMessage(lookupIdArg))
            } catch (exception: Throwable) {
              wrapError(exception)
            }
            reply.reply(wrapped)
          }
        } else {
          channel.setMessageHandler(null)
        }
      }
      run {
        val channel = BasicMessageChannel<Any?>(binaryMessenger, "dev.flutter.pigeon.unify_messages_plus.KamikazeePigeon.deleteContact$separatedMessageChannelSuffix", codec)
        if (api != null) {
          channel.setMessageHandler { message, reply ->
            val args = message as List<Any?>
            val lookupIdArg = args[0] as String
            val wrapped: List<Any?> = try {
              listOf(api.deleteContact(lookupIdArg))
            } catch (exception: Throwable) {
              wrapError(exception)
            }
            reply.reply(wrapped)
          }
        } else {
          channel.setMessageHandler(null)
        }
      }
    }
  }
}
