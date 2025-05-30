package com.example.augmentedrealityglasses.ble.device

import java.util.UUID

val SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")

// transmit data to the device
val CHARACTERISTIC_UUID_TX: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
// receive data from the device
val CHARACTERISTIC_UUID_RX: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")

val DESCRIPTOR_UUID : UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")