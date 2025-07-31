package com.example.augmentedrealityglasses.ble.characteristic.writable

import com.example.augmentedrealityglasses.ble.characteristic.Characteristic

interface WritableCharacteristic : Characteristic {
    /**
     * Write asynchronously the specified value to the characteristic
     */
    suspend fun write(value : String)
}