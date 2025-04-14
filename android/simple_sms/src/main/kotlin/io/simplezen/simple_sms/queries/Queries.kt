package io.simplezen.simple_sms.queries

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import io.flutter.Log
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.SubscriptionManager.DEFAULT_SUBSCRIPTION_ID
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.telephony.TelephonyManagerCompat
import io.simplezen.simple_sms.CuriousPigeon
import io.simplezen.simple_sms.MainActivity
import io.simplezen.simple_sms.QueryObj

// CuriousPigeon
class Query(val context: Context) : CuriousPigeon {

    override fun query(query : QueryObj): List<Map<Any?, Any?>> {
        return getCursorData(context, query).map { it as Map<Any?, Any?> }
    }


    @RequiresPermission(allOf = [Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS])
    override fun getDeviceInfo(): HashMap<String, String> {
        val results = HashMap<String, String>()

        try {
            var brand: String = Build.MANUFACTURER
            var model: String = Build.MODEL
            val os: String = Build.VERSION.SDK_INT.toString()
            val sims : List<Map<Any?, Any?>> = getSimInfo()

            if (model.lowercase().startsWith(brand.lowercase())) {
                model = model.replace(brand, "").trim()
            }
            results["brand"] = brand
            results["model"] = model
            results["os"] = os
            results["sims"] = sims.toString()
        } catch (e: Exception) {
            Log.e("getAllProviders", " <<< Error: ${e.message}")
            e.printStackTrace()
        }
        return results
    }

    @RequiresPermission(allOf = [Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS])
    override fun getSimInfo(): List<Map<Any?, Any?>> {
        val simCards = mutableListOf<Map<String, Any?>>()

        try {
            val telephonyManager : TelephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            val subscriptionManager : SubscriptionManager =
                context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

                val subscriptions = subscriptionManager.activeSubscriptionInfoList ?:  emptyList()
            for (subscription in subscriptions) {
                Log.d("Query", "number " + subscription.number)
                Log.d("Query", "network name : " + subscription.carrierName)
                Log.d("Query", "country iso " + subscription.countryIso)

                val phoneNumber =
                    if(Build.VERSION .SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_PHONE_NUMBERS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            MainActivity.requestPermissions(
                                arrayOf(Manifest.permission.READ_PHONE_NUMBERS)
                            )
                        }
                        subscriptionManager.getPhoneNumber(DEFAULT_SUBSCRIPTION_ID)
                    } else {
                        subscription.number
                    }

                val simCard =
                        mapOf<String, Any?>(
                                "slot" to subscription.simSlotIndex,
                                "phoneNumber" to phoneNumber,
                                "externalId" to -1,
                                "state" to QueryHelper.simStateMap[telephonyManager.getSimState(subscription.simSlotIndex)]?.lowercase() ,
                                "operatorName" to telephonyManager.simOperatorName,
                                "countryIso" to telephonyManager.simCountryIso,
                                "imei" to TelephonyManagerCompat.getImei(telephonyManager),
//                                "serialNumber" to telephonyManager.getSimSerialNumber(),
                                "carrierName" to telephonyManager.simOperatorName,
                                "isNetworkRoaming" to telephonyManager.isNetworkRoaming
                        )
                simCards.add(simCard)
            }
            Log.d("SimsQuery", "SIM cards: $simCards")
            return simCards as List<Map<Any?, Any?>>

        } catch (e: Exception) {
            e.printStackTrace()
            val errorPigeon =
                    mapOf<String, Any?>(
                        "slot" to -1,
                        "externalId" to "-1",
                        "state" to "UNKNOWN",
                        "operatorName" to "UNKNOWN",
                        "countryIso" to "UNKNOWN",
                        "serialNumber" to "UNKNOWN",
                        "carrierName" to "UNKNOWN",
                        "displayName" to "UNKNOWN",
                        "error" to e.message.toString(),
                        "isNetworkRoaming" to false
                    )
            return listOf(errorPigeon) as List<Map<Any?, Any?>>
        }
    }


    private fun getCursorData(context : Context, query : QueryObj): List<Map<String, Any?>> {

        val contentResolver = context.contentResolver

        val contentUri = query.contentUri.toUri()
        val projection = query.projection?.toTypedArray()
        val selection = query.selection
        val selectionArgs = query.selectionArgs?.toTypedArray()
        val sortOrder = query.sortOrder

        val cursor: Cursor = contentResolver.query(contentUri, projection, selection, selectionArgs, sortOrder) ?: return emptyList()

        val returnable: MutableList<Map<String, Any?>> = mutableListOf()
        val columnMap : HashMap<String, Int> = cursor.columnNames.associateWith { 0 }.toMap() as HashMap<String, Int>
        cursor.use {
            while (it.moveToNext()) {
                val row = HashMap<String, Any?>()
                for (index in 0 until cursor.columnCount) {
                    val columnName = cursor.getColumnName(index)
                    val columnType = cursor.getType(index)
                    when (columnType) {
                        Cursor.FIELD_TYPE_NULL -> row[columnName] = ""
                        Cursor.FIELD_TYPE_INTEGER -> row[columnName] = cursor.getLong(index)
                        Cursor.FIELD_TYPE_FLOAT -> row[columnName] = cursor.getFloat(index)
                        Cursor.FIELD_TYPE_STRING -> row[columnName] = cursor.getString(index)
                        Cursor.FIELD_TYPE_BLOB -> row[columnName] = cursor.getBlob(index)
                        else -> {
                            throw Exception("Unknown column type: $columnType")
                        }
                    }
                    val valLength = row[columnName].toString().length
                    if (valLength > columnMap[columnName].toString().toInt()) {
                        columnMap[columnName] = valLength
                    }
                }
                if (row.isEmpty()) {
                    continue
                }
                returnable.add(row)
            }
                //returnable.add(columnMap as HashMap<String, Any?>)
        }
        // Log.d("getAllCursorData", " <<< Returning: $returnable")
        return returnable
    }

//    private fun _printQueryResults(objectName: String, results: Map<String, Any?>) {
//        val sortedMap = results.toSortedMap(compareBy<String> { it }.thenBy { it.length })
//
//        Log.d("printQueryResults", " <<< ")
//        Log.d("printQueryResults", " <<< -------------------------------")
//        Log.d("printQueryResults", " <<<    >>>> $objectName Data ")
//        Log.d("printQueryResults", " <<< -------------------------------")
//        Log.d("printQueryResults", " <<< ")
//        for (key in sortedMap.keys) Log.d("printQueryResults", " <<< $key: ${sortedMap[key]}")
//        Log.d("printQueryResults", " <<< ")
//        Log.d("printQueryResults", " <<< ")
//        Log.d("printQueryResults", " <<< ")
//    }
}

private class QueryHelper() {
    companion object {
     val simStateMap: HashMap<Int, String> =
        hashMapOf(
                TelephonyManager.SIM_STATE_UNKNOWN to "UNKNOWN",
                TelephonyManager.SIM_STATE_ABSENT to "ABSENT",
                TelephonyManager.SIM_STATE_PIN_REQUIRED to "PIN_REQUIRED",
                TelephonyManager.SIM_STATE_PUK_REQUIRED to "PUK_REQUIRED",
                TelephonyManager.SIM_STATE_NETWORK_LOCKED to "NETWORK_LOCKED",
                TelephonyManager.SIM_STATE_READY to "READY",
                TelephonyManager.SIM_STATE_NOT_READY to "NOT_READY",
                TelephonyManager.SIM_STATE_PERM_DISABLED to "PERM_DISABLED",
                TelephonyManager.SIM_STATE_CARD_IO_ERROR to "CARD_IO_ERROR",
                TelephonyManager.SIM_STATE_CARD_RESTRICTED to "CARD_RESTRICTED"
        )
    }
}