package com.example.augmentedrealityglasses

import android.app.Application
import android.util.Log

class App : Application() {
    private val TAG = "myapp"
    /** AppContainer instance used by the rest of classes to obtain dependencies */
    lateinit var container: AppContainer
    override fun onCreate() {
        Log.d(TAG, "Creating the application")
        super.onCreate()
        // application context passed since the dependencies in DefaultAppContainer live as long as the application
        container = DefaultAppContainer(this)
    }
}