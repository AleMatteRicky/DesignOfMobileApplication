package com.example.augmentedrealityglasses.callnotifications

import android.app.NotificationManager
import android.util.Log
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.example.augmentedrealityglasses.App
import com.example.augmentedrealityglasses.ble.device.RemoteDeviceManager

class PhoneCallReceiver : BroadcastReceiver() {
    private val TAG: String = "PhoneCallReceiver"

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Calling on receive")
        val bleManager: RemoteDeviceManager =
            (context.applicationContext as App).container.bleManager

        val incomingCall = "incoming_call"

        intent?.let {
            val action: String? = it.action

            // check if DND is active
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val filter = notificationManager.currentInterruptionFilter
            if (isDNDActive(filter)) {
                Log.d(TAG, "DND is active: do not share the notification")
                return
            }

            if (TelephonyManager.ACTION_PHONE_STATE_CHANGED == action) {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                if (TelephonyManager.EXTRA_STATE_RINGING == state) {
                    Log.d(TAG, "Incoming call")
                    if (bleManager.isConnected()) {
                        bleManager.send(incomingCall)
                    } else {
                        Log.d(
                            TAG,
                            "Incoming call event not transmitted to the device because they are offline"
                        )
                    }
                } else{
                    Log.d(TAG, "Not interesting state")
                }
            } else{
                Log.d(TAG, "Broadcast has received an unrecognised action $action")
            }
        }
    }

    private fun isDNDActive(filter: Int): Boolean {
        return filter == NotificationManager.INTERRUPTION_FILTER_NONE
                || filter == NotificationManager.INTERRUPTION_FILTER_ALARMS
                || filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY
    }
}