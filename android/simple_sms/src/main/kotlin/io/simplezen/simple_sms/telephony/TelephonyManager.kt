package io.simplezen.simple_sms.telephony

import android.content.Context
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import java.lang.reflect.InvocationTargetException


class TelephonyManager(private val context: Context) {
    private var manager: TelephonyManager? = null
        get() {
            if (field == null) {
                field = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            }
            return field
        }

    val simCount: Int
        get() = manager!!.phoneCount

    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    fun getSimId(slotId: Int): String {
        return manager!!.getDeviceId(slotId)
    }

    fun getSimState(slotId: Int): Int {
        try {
            val getSimStateMethod = manager!!.javaClass.getMethod(
                "getSimState",
                Int::class.javaPrimitiveType
            )
            val result = getSimStateMethod.invoke(manager, slotId)
            if (result != null) {
                return result as Int
            }
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
        return TelephonyManager.SIM_STATE_UNKNOWN
    }
}
