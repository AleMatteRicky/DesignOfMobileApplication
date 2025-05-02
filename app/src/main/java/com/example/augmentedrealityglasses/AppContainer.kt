package com.example.augmentedrealityglasses

import android.content.Context
import com.example.augmentedrealityglasses.ble.device.BleManager

/**
 * Dependency Injection container at the application level.
 */
interface AppContainer {
    val bleManager: BleManager
}

/**
 * Container for all variables shared across the whole app.
 */
class DefaultAppContainer(
    context: Context
) : AppContainer {
    override val bleManager: BleManager =
        BleManager(context)
}