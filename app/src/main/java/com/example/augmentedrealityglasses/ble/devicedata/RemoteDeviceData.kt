package com.example.augmentedrealityglasses.ble.devicedata

import com.example.augmentedrealityglasses.ble.peripheral.gattevent.ConnectionState

data class RemoteDeviceData(
    val connectionState: ConnectionState, // state of the connection with the peripheral
    val messageReceived: String = "", // message coming from the peripheral
) {
    companion object {
        val None = RemoteDeviceData(ConnectionState.Initial)
    }
}