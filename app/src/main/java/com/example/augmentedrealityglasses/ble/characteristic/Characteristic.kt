package com.example.augmentedrealityglasses.ble.characteristic

import android.bluetooth.BluetoothGattCharacteristic
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

    /**
     * Maximum bytes that can be contained in this characteristic
     */
    val maximumLength : Int

    enum class CharacteristicProperty {
        WRITE, // can be written
        SIGNED_WRITE, // encrypted => pairing if written
        WRITE_WITHOUT_RESPONSE, // write, no need for ack back
        READ, // can be actively read (pull notification)
        INDICATE, // do not send an ack back to the peripheral (push notification)
        NOTIFY, // do send an ack back from the peripheral (push notification)
    }

    companion object {
        fun isWritable(props: Set<CharacteristicProperty>): Boolean {
            return props.contains(CharacteristicProperty.WRITE)
                    || props.contains(CharacteristicProperty.SIGNED_WRITE)
                    || props.contains(CharacteristicProperty.WRITE_WITHOUT_RESPONSE)
        }

        fun isReadable(props: Set<CharacteristicProperty>): Boolean {
            return props.contains(CharacteristicProperty.READ)
                    || props.contains(CharacteristicProperty.INDICATE)
                    || props.contains(CharacteristicProperty.NOTIFY)
        }
    }
}

fun Int.toCharacteristicProperties(): Set<Characteristic.CharacteristicProperty> {
    val properties = mutableSetOf<Characteristic.CharacteristicProperty>()

    if (this and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
        properties.add(Characteristic.CharacteristicProperty.INDICATE)
    }
    if (this and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
        properties.add(Characteristic.CharacteristicProperty.NOTIFY)
    }
    if (this and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
        properties.add(Characteristic.CharacteristicProperty.READ)
    }
    if (this and BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE != 0) {
        properties.add(Characteristic.CharacteristicProperty.SIGNED_WRITE)
    }
    if (this and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
        properties.add(Characteristic.CharacteristicProperty.WRITE)
    }
    if (this and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
        properties.add(Characteristic.CharacteristicProperty.WRITE_WITHOUT_RESPONSE)
    }

    return properties
}