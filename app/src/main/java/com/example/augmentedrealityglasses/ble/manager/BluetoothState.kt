package com.example.augmentedrealityglasses.ble.manager

import android.bluetooth.BluetoothAdapter

enum class BluetoothState {
    ON,
    OFF,
    TURNING_ON,
    TURNING_OFF;
}

fun Int.toBluetoothState() : BluetoothState = when(this) {
    BluetoothAdapter.STATE_ON -> BluetoothState.ON
    BluetoothAdapter.STATE_OFF -> BluetoothState.OFF
    BluetoothAdapter.STATE_TURNING_ON -> BluetoothState.TURNING_ON
    else -> BluetoothState.TURNING_OFF
}