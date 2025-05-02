package com.example.augmentedrealityglasses

import android.content.Context
import com.example.augmentedrealityglasses.ble.device.BleManager
import com.example.augmentedrealityglasses.ble.device.RemoteDeviceManager

/**
 * Dependency Injection container at the application level.
 */
interface AppContainer {
    val bleManager: RemoteDeviceManager
}

/**
 * Container for all variables shared across the whole app.
 */
class DefaultAppContainer(
    context: Context
) : AppContainer {
    override val bleManager: RemoteDeviceManager =
        BleManager(context)
}