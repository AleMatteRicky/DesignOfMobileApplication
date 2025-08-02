package com.example.augmentedrealityglasses.ble.characteristic.writable.message

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

data class Message(
    val data: ByteArray,
    val gatt: BluetoothGatt,
    val characteristic: BluetoothGattCharacteristic,
    val state: State = State.ToSend,
    val nTries: UInt = 0U
) {
    enum class State {
        ToSend,
        Pending,
    }
}