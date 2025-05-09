package com.example.augmentedrealityglasses

import android.app.Application
import android.content.IntentFilter
import android.telephony.TelephonyManager
import android.util.Log
import com.example.augmentedrealityglasses.notifications.PhoneCallReceiver

class App : Application() {
    private val TAG = "myapp"

    /** AppContainer instance used by the rest of classes to obtain dependencies */
    lateinit var container: AppContainer
    override fun onCreate() {
        Log.d(TAG, "Creating the application")
        super.onCreate()
        // application context passed since the dependencies in DefaultAppContainer live as long as the application
        container = DefaultAppContainer(this)
        // register the broadcast receiver
        val receiver = PhoneCallReceiver()
        registerReceiver(receiver, IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED))
    }
}