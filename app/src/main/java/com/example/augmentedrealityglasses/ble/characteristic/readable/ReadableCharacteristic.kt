package com.example.augmentedrealityglasses.ble.characteristic.readable

import com.example.augmentedrealityglasses.ble.characteristic.Characteristic
import kotlinx.coroutines.flow.Flow

interface ReadableCharacteristic : Characteristic {
    /**
     * Subscribe to listen for updates related to the characteristic.
     * @return state flow containing the last message coming from the remote device
     */
    fun subscribe() : Flow<String>
}