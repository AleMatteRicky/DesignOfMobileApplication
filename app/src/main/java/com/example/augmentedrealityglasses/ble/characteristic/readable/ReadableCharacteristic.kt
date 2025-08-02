package com.example.augmentedrealityglasses.ble.characteristic.readable

import com.example.augmentedrealityglasses.ble.characteristic.Characteristic
import kotlinx.coroutines.flow.Flow

interface ReadableCharacteristic : Characteristic {
    /**
     * Subscribe to listen for updates related to the characteristic.
     * @return state flow containing the last message coming from the remote device
     */
    fun subscribe(): Flow<ByteArray>

    /**
     * suspend the caller coroutine until the current characteristic value is read or the characteristic
     * is invalidated
     * @return the current value of the characteristic or null if the operation was not successful
     */
    suspend fun read(): ByteArray?

    /**
     * Enable notification or indication of the characteristic
     * @return true if the characteristic's notification/indication state has been set correctly, false otherwise
     */
    suspend fun setNotify(enable: Boolean) : Boolean
}