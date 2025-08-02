package com.example.augmentedrealityglasses.ble.peripheral.gattevent

import android.bluetooth.BluetoothGattService

data object ServiceChangedEvent : GattEvent()

data class ServiceDiscoveredEvent(
    val services: List<BluetoothGattService>? // null if the discovery failed
) : GattEvent()