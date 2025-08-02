package com.example.augmentedrealityglasses.ble.peripheral.gattevent

import android.bluetooth.BluetoothStatusCodes
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.Status.FAILURE
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.Status.SUCCESS

enum class Status {
    SUCCESS,
    FAILURE;
}

fun Int.genStatus() : Status {
    return if(this == BluetoothStatusCodes.SUCCESS) {
        SUCCESS
    } else {
        FAILURE
    }
}