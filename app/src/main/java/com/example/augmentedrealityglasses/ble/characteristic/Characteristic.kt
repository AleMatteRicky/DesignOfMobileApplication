package com.example.augmentedrealityglasses.ble.characteristic

import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE
import java.util.UUID

interface Characteristic {
    /**
     * UUID for the characteristic
     */
    val uuid: UUID

    /**
     * Property of this characteristic
     */
    val properties: Set<CharacteristicProperty>

    enum class CharacteristicProperty(val intRep : Int) {
        WRITE(PROPERTY_WRITE),
        READ(PROPERTY_READ),
        INDICATE(PROPERTY_INDICATE), // receive ack back from the peripheral
        NOTIFY (PROPERTY_NOTIFY)// do not receive an ack back from the peripheral
    }
}

fun Int.toCharacteristicProperties(): Set<Characteristic.CharacteristicProperty> {
    val properties = mutableSetOf<Characteristic.CharacteristicProperty>()
    for (property in Characteristic.CharacteristicProperty.entries) {
        if(this.and(property.intRep) != 0) {
            properties.add(property)
        }
    }
    return properties
}