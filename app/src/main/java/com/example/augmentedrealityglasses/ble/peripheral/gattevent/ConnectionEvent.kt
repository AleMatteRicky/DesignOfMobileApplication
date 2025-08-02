package com.example.augmentedrealityglasses.ble.peripheral.gattevent

import android.bluetooth.BluetoothGatt
import com.example.augmentedrealityglasses.ble.peripheral.bonding.BondState
import com.example.augmentedrealityglasses.ble.peripheral.bonding.toBondState

class ConnectionEvent(
    val state: ConnectionState
) : GattEvent()

sealed class ConnectionState {
    data object Closed : ConnectionState()
    data object Connecting : ConnectionState()
    data class Connected(val bondState: BondState): ConnectionState()
    data object Disconnecting : ConnectionState()
    data class Disconnected(val reason: String, val userInitiatedDisconnection: Boolean) :
        ConnectionState()
}

fun Int.toConnectionState(status: Int, bondState: Int): ConnectionState =
    if (status == BluetoothGatt.GATT_SUCCESS) {
        when (this) {
            BluetoothGatt.STATE_DISCONNECTED -> ConnectionState.Disconnected("$status", true)
            BluetoothGatt.STATE_CONNECTED -> ConnectionState.Connected(bondState.toBondState())
            BluetoothGatt.STATE_CONNECTING -> ConnectionState.Connecting
            BluetoothGatt.STATE_DISCONNECTING -> ConnectionState.Disconnecting
            else -> ConnectionState.Disconnected("$status", true)
        }
    } else {
        ConnectionState.Disconnected("$status", false)
    }