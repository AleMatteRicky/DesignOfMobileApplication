package com.example.augmentedrealityglasses.notifications

import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.provider.Telephony.Sms.Intents.getMessagesFromIntent
import android.util.Log
import com.example.augmentedrealityglasses.ble.device.RemoteDeviceManager

class SmsReceiver : NotificationReceiver() {
    private val TAG: String = "SmsReceiver"

    override fun pushNotification(context: Context, intent: Intent?, bleManager : RemoteDeviceManager) {
        intent?.let {
            if (isSmsReceived(intent)) {
                val messages = getMessagesFromIntent(intent)
                for (sms in messages) {
                    val body = sms.messageBody
                    val sender = sms.originatingAddress
                    Log.d(TAG, "Sending message $body from $sender")
                    bleManager.send("sender: $sender body: $body")
                }
            }
        }
    }

    private fun isSmsReceived(intent: Intent): Boolean {
        return Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action
    }
}