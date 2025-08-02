package com.example.augmentedrealityglasses.ble.service

import com.example.augmentedrealityglasses.ble.characteristic.readable.ReadableCharacteristic
import com.example.augmentedrealityglasses.ble.characteristic.writable.WritableCharacteristic
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

interface Service {
    /**
     * UUID of the service
     */
    val uuid : UUID

    /**
     * Flow to know whether the service is available or not. If a service is not available is because
     * either the remote device is not connected, or the service itself has not been discovered yet
     */
    val isAvailable : StateFlow<Boolean>

    /**
     * List of writable characteristics supported by the service
     */
    val writableCharacteristics : List<WritableCharacteristic>

    /**
     * List of readable characteristics supported by the service
     */
    val readableCharacteristics : List<ReadableCharacteristic>

    /**
     * Stop the service
     */
    fun stopService()
}