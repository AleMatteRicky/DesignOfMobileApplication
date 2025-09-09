package com.example.augmentedrealityglasses.notifications

import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.provider.Telephony.Sms.Intents.getMessagesFromIntent
import android.util.Log
import com.example.augmentedrealityglasses.ble.devicedata.RemoteDeviceManager
import com.example.augmentedrealityglasses.settings.NotificationSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver(
    val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : NotificationReceiver() {
    private val TAG: String = "SmsReceiver"

    override fun pushNotification(
        context: Context,
        intent: Intent?,
        proxy: RemoteDeviceManager
    ) {
        intent?.let {
            if (isSmsReceived(intent)) {
                val messages = getMessagesFromIntent(intent)
                for (sms in messages) {
                    val body = sms.messageBody
                    val address = sms.originatingAddress

                    val sender = when {
                        !address.isNullOrBlank() -> getContactNameOrNull(context, address)
                            ?: address

                        else -> "Unknown"
                    }

                    scope.launch {
                        if (isNotificationSourceEnabled(context, NotificationSource.SMS)) {
                            if (proxy.isConnected()) {

                                val msg = createBLEMessage(
                                    command = "n",
                                    source = NotificationSource.SMS,
                                    sender = sender,
                                    content = body
                                )

                                Log.d(TAG, "Ble message sent:\n$msg")

                                proxy.send(msg)
                            } else {
                                Log.d(
                                    TAG,
                                    "Incoming sms event not transmitted to the device because they are offline"
                                )
                            }
                        } else {
                            Log.d(TAG, "Sms notifications disabled")
                        }
                    }
                }
            }
        }
    }

    private fun isSmsReceived(intent: Intent): Boolean {
        return Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action
    }
}