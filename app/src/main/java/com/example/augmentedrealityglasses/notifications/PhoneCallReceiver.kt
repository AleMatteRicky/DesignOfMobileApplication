package com.example.augmentedrealityglasses.notifications

import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.augmentedrealityglasses.ble.devicedata.RemoteDeviceManager
import com.example.augmentedrealityglasses.settings.NotificationSource
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

        intent?.let {
            val action: String? = it.action

            if (TelephonyManager.ACTION_PHONE_STATE_CHANGED == action) {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                if (TelephonyManager.EXTRA_STATE_RINGING == state) {

                    Log.d(TAG, "Incoming call")

                    val incomingNumber =
                        intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

                    if (incomingNumber != null) {
                        val sender = getContactNameOrNull(context, incomingNumber) ?: incomingNumber

                        scope.launch {
                            if (isNotificationSourceEnabled(context, NotificationSource.CALL)) {
                                Log.d(TAG, "Phone number/contact name: $sender")
                                if (proxy.isConnected()) {

                                    val msg = createBLEMessage(
                                        command = "n",
                                        source = NotificationSource.CALL,
                                        sender = sender,
                                        content = "Incoming call"
                                    )

                                    Log.d(TAG, "Ble message sent:\n$msg")
                                    proxy.send(msg)
                                } else {
                                    Log.d(
                                        TAG,
                                        "Incoming call event not transmitted to the device because they are offline"
                                    )
                                }
                            } else {
                                Log.d(TAG, "Call notifications disabled")
                            }
                        }
                    } else {
                        Log.d(TAG, "Null phone number")
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