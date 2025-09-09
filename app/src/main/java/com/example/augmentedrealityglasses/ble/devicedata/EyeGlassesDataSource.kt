package com.example.augmentedrealityglasses.ble.devicedata

import java.util.UUID

val SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")

// transmit data to the device
val CHARACTERISTIC_UUID_TX: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
// receive data from the device
val CHARACTERISTIC_UUID_RX: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")

val DESCRIPTOR_UUID : UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")