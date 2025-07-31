package com.example.augmentedrealityglasses.ble.peripheral.gattevent

sealed class GattEvent {
    /** Whether the event notifies about a disconnection. */
    val isDisconnectionEvent: Boolean
        get() = this is ConnectionEvent && state == ConnectionState.DISCONNECTED

    /** Whether the event notifies about services change, including disconnection. */
    val isServiceInvalidatedEvent: Boolean
        get() = isDisconnectionEvent || this is ServiceChangedEvent
}