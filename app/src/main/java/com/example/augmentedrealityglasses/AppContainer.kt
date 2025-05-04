package com.example.augmentedrealityglasses

import android.content.Context
import com.example.augmentedrealityglasses.ble.device.BleManager
import com.example.augmentedrealityglasses.weather.retrofit.WeatherRepositoryImpl

/**
 * Dependency Injection container at the application level.
 */
interface AppContainer {
    val bleManager: BleManager
    val weatherAPIRepository: WeatherRepositoryImpl
}

/**
 * Container for all variables shared across the whole app.
 */
class DefaultAppContainer(
    context: Context
) : AppContainer {
    override val bleManager: BleManager =
        BleManager(context)
    override val weatherAPIRepository: WeatherRepositoryImpl =
        WeatherRepositoryImpl()
}