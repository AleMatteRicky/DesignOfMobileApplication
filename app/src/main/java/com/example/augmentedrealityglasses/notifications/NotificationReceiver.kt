package com.example.augmentedrealityglasses.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.augmentedrealityglasses.App
import com.example.augmentedrealityglasses.ble.devicedata.RemoteDeviceManager
import com.example.augmentedrealityglasses.settings.NotificationSource
import org.json.JSONObject

abstract class NotificationReceiver : BroadcastReceiver() {
    private val TAG: String = "NotificationReceiver"

    final override fun onReceive(context: Context, intent: Intent?) {
        if (!isDNDActive(context)) {
            val proxy: RemoteDeviceManager =
                (context.applicationContext as App).container.proxy
            pushNotification(context, intent, proxy)
        } else {
            Log.d(TAG, "Notification discarded: DND mode is active")
        }
    }

    abstract fun pushNotification(
        context: Context,
        intent: Intent?,
        bleManager: RemoteDeviceManager
    )

    private fun isDNDActive(context: Context): Boolean {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val filter = notificationManager.currentInterruptionFilter
        return filter == NotificationManager.INTERRUPTION_FILTER_NONE
                || filter == NotificationManager.INTERRUPTION_FILTER_ALARMS
                || filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY
    }

    suspend fun isNotificationSourceEnabled(context: Context, source: NotificationSource): Boolean {
        return NotificationFilters.isEnabled(context, source)
    }

    /**
     * This function fetch the contact name associated to a phone number if READ_CONTACTS permission is granted.
     * It returns null in case permissions are not granted or the phone number is unknown to the device
     */
    fun getContactNameOrNull(context: Context, phoneNumber: String): String? {
        val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.READ_CONTACTS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasPerm) return null

        val uri = android.net.Uri.withAppendedPath(
            android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(phoneNumber)
        )

        context.contentResolver.query(
            uri,
            arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME),
            null, null, null
        ).use { c ->
            if (c != null && c.moveToFirst()) {
                val idx =
                    c.getColumnIndex(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)
                if (idx >= 0) return c.getString(idx)
            }
        }
        return null
    }

    fun createBLEMessage(
        command: String,
        source: NotificationSource,
        sender: String,
        content: String
    ): String {
        val jsonToSend = JSONObject()

        jsonToSend.put("command", command)
        jsonToSend.put("source", source.toString().lowercase())
        jsonToSend.put("sender", sender)
        jsonToSend.put("content", content)

        val msg = jsonToSend.toString()

        return msg
    }
}