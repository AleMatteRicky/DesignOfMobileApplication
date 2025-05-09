package com.example.augmentedrealityglasses

import android.app.Application
import android.content.IntentFilter
import android.provider.Telephony
import android.telephony.TelephonyManager
import android.util.Log
import com.example.augmentedrealityglasses.notifications.PhoneCallReceiver
import com.example.augmentedrealityglasses.notifications.SmsReceiver

class App : Application() {
    private val TAG = "myapp"

    /** AppContainer instance used by the rest of classes to obtain dependencies */
    lateinit var container: AppContainer
    override fun onCreate() {
        Log.d(TAG, "Creating the application")
        super.onCreate()
        // application context passed since the dependencies in DefaultAppContainer live as long as the application
        container = DefaultAppContainer(this)
        // register the broadcast receivers
        registerReceiver(
            PhoneCallReceiver(),
            IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        )

        if (container.isDeviceSmsCapable) {
            registerReceiver(SmsReceiver(), IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION))
        }
    }
}