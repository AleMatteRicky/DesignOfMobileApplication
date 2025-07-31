package com.example.augmentedrealityglasses.ble.peripheral.gattevent

class ConnectionEvent(
    val state : ConnectionState
) : GattEvent()

enum class ConnectionState {
    BONDING,
    BONDED,
    CONNECTED,
    DISCONNECTED,
    CLOSED // use this state to indicate that the connection has been closed, useful as terminator signal for all the consumers of GattEvents
}