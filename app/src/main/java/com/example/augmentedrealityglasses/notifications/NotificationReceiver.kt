package com.example.augmentedrealityglasses.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.augmentedrealityglasses.App
import com.example.augmentedrealityglasses.ble.device.RemoteDeviceManager

abstract class NotificationReceiver : BroadcastReceiver() {
    private val TAG: String = "NotificationReceiver"

    final override fun onReceive(context: Context, intent: Intent?) {
        if (!isDNDActive(context)) {
            val bleManager: RemoteDeviceManager =
                (context.applicationContext as App).container.bleManager
            pushNotification(context, intent, bleManager)
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
}