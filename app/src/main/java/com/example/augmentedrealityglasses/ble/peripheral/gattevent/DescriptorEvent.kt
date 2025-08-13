package com.example.augmentedrealityglasses.ble.peripheral.gattevent

import android.bluetooth.BluetoothGattDescriptor

data class DescriptorWriteEvent(
    val descriptor : BluetoothGattDescriptor,
    val status: Status
): GattEvent()