package com.example.augmentedrealityglasses.ble.peripheral.gattevent

import android.bluetooth.BluetoothGattService
import com.example.augmentedrealityglasses.ble.characteristic.writable.message.Message

data object ServiceChangedEvent : GattEvent()

data class ServiceDiscoveredEvent(
    val state: Message.State,
    val services: List<BluetoothGattService>
) : GattEvent()