package com.example.augmentedrealityglasses.ble.characteristic

import java.util.UUID

interface Characteristic {
    /**
     * UUID for the characteristic
     */
    val uuid : UUID

    /**
     * Property of this characteristic
     */
    val properties : List<CharacteristicProperty>

    enum class CharacteristicProperty {
        WRITE,
        READ,
        INDICATE, // receive ack back from the peripheral
        NOTIFY // do not receive an ack back from the peripheral
    }
}