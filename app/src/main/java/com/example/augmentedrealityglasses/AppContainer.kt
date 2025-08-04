package com.example.augmentedrealityglasses

import android.content.Context
import com.example.augmentedrealityglasses.ble.ESP32Proxy
import com.example.augmentedrealityglasses.weather.network.WeatherRepositoryImpl
import com.example.augmentedrealityglasses.ble.device.RemoteDeviceManager

/**
 * Dependency Injection container at the application level.
 */
interface AppContainer {
    val proxy: RemoteDeviceManager
    val weatherAPIRepository: WeatherRepositoryImpl
}

/**
 * Container for all variables shared across the whole app.
 */
class DefaultAppContainer(
    context: Context
) : AppContainer {
    override val proxy: RemoteDeviceManager =
        ESP32Proxy(context)
    override val weatherAPIRepository: WeatherRepositoryImpl =
        WeatherRepositoryImpl()
}