package com.example.augmentedrealityglasses.container

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import com.example.augmentedrealityglasses.home.BootstrapPrefs
import com.example.augmentedrealityglasses.ble.ESP32Proxy
import com.example.augmentedrealityglasses.ble.devicedata.RemoteDeviceManager
import com.example.augmentedrealityglasses.cache.Cache
import com.example.augmentedrealityglasses.cache.CachePolicy
import com.example.augmentedrealityglasses.cache.DataStoreMapCache
import com.example.augmentedrealityglasses.cache.MaxAgePolicy
import com.example.augmentedrealityglasses.cache.NeverExpires
import com.example.augmentedrealityglasses.weather.constants.Constants
import com.example.augmentedrealityglasses.weather.network.WeatherRepositoryImpl
import java.io.File

/**
 * Dependency Injection container at the application level.
 */
interface AppContainer {
    val proxy: RemoteDeviceManager
    val weatherAPIRepository: WeatherRepositoryImpl
    val weatherCache: Cache
    val weatherCachePolicy: CachePolicy
    val isDeviceSmsCapable: Boolean
    val settingsCache: Cache
    val settingsCachePolicy: CachePolicy
    val bootstrapPrefs: BootstrapPrefs
}

/**
 * Container for all variables shared across the whole app.
 */
class DefaultAppContainer(
    context: Context
) : AppContainer {
    override val proxy: RemoteDeviceManager =
        ESP32Proxy(context)

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    override val isDeviceSmsCapable: Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            telephonyManager.isDeviceSmsCapable
        } else {
            telephonyManager.isSmsCapable
        }
    override val weatherAPIRepository: WeatherRepositoryImpl =
        WeatherRepositoryImpl()
    override val weatherCache: Cache =
        DataStoreMapCache(File(context.applicationContext.filesDir, "weather_cache.json"))
    override val weatherCachePolicy: CachePolicy =
        MaxAgePolicy(Constants.MAX_AGE_WEATHER_CACHE_POLICY_MILLS)
    override val settingsCache: Cache =
        DataStoreMapCache(File(context.applicationContext.filesDir, "settings_cache.json"))
    override val settingsCachePolicy: CachePolicy =
        NeverExpires
    override val bootstrapPrefs: BootstrapPrefs = BootstrapPrefs(context)
}