package com.example.augmentedrealityglasses

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import com.example.augmentedrealityglasses.ble.device.BleManager
import com.example.augmentedrealityglasses.ble.device.RemoteDeviceManager

/**
 * Dependency Injection container at the application level.
 */
interface AppContainer {
    val bleManager: RemoteDeviceManager
    val isDeviceSmsCapable: Boolean
}

/**
 * Container for all variables shared across the whole app.
 */
class DefaultAppContainer(
    context: Context
) : AppContainer {
    override val bleManager: RemoteDeviceManager =
        BleManager(context)
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    override val isDeviceSmsCapable: Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            telephonyManager.isDeviceSmsCapable
        } else {
            telephonyManager.isSmsCapable
        }
}