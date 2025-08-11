package com.example.augmentedrealityglasses

import android.content.Context
import com.example.augmentedrealityglasses.ble.ESP32Proxy
import com.example.augmentedrealityglasses.ble.devicedata.RemoteDeviceManager
import com.example.augmentedrealityglasses.cache.Cache
import com.example.augmentedrealityglasses.cache.CachePolicy
import com.example.augmentedrealityglasses.cache.DataStoreMapCache
import com.example.augmentedrealityglasses.cache.MaxAgePolicy
import com.example.augmentedrealityglasses.weather.constants.Constants
import com.example.augmentedrealityglasses.weather.network.WeatherRepositoryImpl

/**
 * Dependency Injection container at the application level.
 */
interface AppContainer {
    val proxy: RemoteDeviceManager
    val weatherAPIRepository: WeatherRepositoryImpl
    val weatherCache: Cache
    val weatherCachePolicy: CachePolicy
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
    override val weatherCache: Cache = DataStoreMapCache(context, "weather_cache.json")
    override val weatherCachePolicy: CachePolicy =
        MaxAgePolicy(Constants.MAX_AGE_WEATHER_CACHE_POLICY_MILLS)
}