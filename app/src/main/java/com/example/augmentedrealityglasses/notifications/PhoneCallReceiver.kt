package com.example.augmentedrealityglasses.notifications

import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.augmentedrealityglasses.ble.devicedata.RemoteDeviceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PhoneCallReceiver(
    val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : NotificationReceiver() {
    private val TAG: String = "PhoneCallReceiver"

    override fun pushNotification(
        context: Context,
        intent: Intent?,
        proxy: RemoteDeviceManager
    ) {
        Log.d(TAG, "Calling on receive")

        val incomingCall = "incoming_call"

        intent?.let {
            val action: String? = it.action

            if (TelephonyManager.ACTION_PHONE_STATE_CHANGED == action) {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                if (TelephonyManager.EXTRA_STATE_RINGING == state) {
                    Log.d(TAG, "Incoming call")
                    if (proxy.isConnected()) {
                        scope.launch {
                            proxy.send(incomingCall)
                        }
                    } else {
                        Log.d(
                            TAG,
                            "Incoming call event not transmitted to the device because they are offline"
                        )
                    }
                } else {
                    Log.d(TAG, "Not interesting state")
                }
            } else {
                Log.d(TAG, "Broadcast has received an unrecognised action $action")
            }
        }
    }
}