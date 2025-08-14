package com.example.augmentedrealityglasses.notifications

import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.provider.Telephony.Sms.Intents.getMessagesFromIntent
import android.util.Log
import com.example.augmentedrealityglasses.ble.devicedata.RemoteDeviceManager
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
                    val sender = sms.originatingAddress
                    Log.d(TAG, "Sending message $body from $sender")
                    scope.launch {
                        proxy.send("sender: $sender body: $body")
                    }
                }
            }
        }
    }

    private fun isSmsReceived(intent: Intent): Boolean {
        return Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action
    }
}