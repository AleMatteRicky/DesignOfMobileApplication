package com.example.augmentedrealityglasses.ble.peripheral.gattevent

import android.bluetooth.BluetoothGattCharacteristic

data class CharacteristicWriteEvent(
    val status: Status,
    val characteristic: BluetoothGattCharacteristic
) : GattEvent()

data class CharacteristicChangedEvent(
    val characteristic: BluetoothGattCharacteristic,
    val value: ByteArray
) : GattEvent()

data class CharacteristicReadEvent(
    val status: Status,
    val characteristic: BluetoothGattCharacteristic,
    val value: ByteArray
) : GattEvent()