package com.example.augmentedrealityglasses.ble.peripheral.gattevent

import android.bluetooth.BluetoothGatt
import com.example.augmentedrealityglasses.ble.peripheral.bonding.toBondState
import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectionStateTest {

    @Test
    fun `returns Disconnected when status is GATT_SUCCESS and state is STATE_DISCONNECTED`() {
        val result = BluetoothGatt.STATE_DISCONNECTED.toConnectionState(
            status = BluetoothGatt.GATT_SUCCESS,
            bondState = 0
        )
        assertEquals(
            ConnectionState.Disconnected("${BluetoothGatt.GATT_SUCCESS}", true),
            result
        )
    }

    @Test
    fun `returns Connected when status is GATT_SUCCESS and state is STATE_CONNECTED`() {
        val bondState = 1
        val result = BluetoothGatt.STATE_CONNECTED.toConnectionState(
            status = BluetoothGatt.GATT_SUCCESS,
            bondState = bondState
        )
        assertEquals(
            ConnectionState.Connected(bondState.toBondState()),
            result
        )
    }

    @Test
    fun `returns Connecting when status is GATT_SUCCESS and state is STATE_CONNECTING`() {
        val result = BluetoothGatt.STATE_CONNECTING.toConnectionState(
            status = BluetoothGatt.GATT_SUCCESS,
            bondState = 0
        )
        assertEquals(ConnectionState.Connecting, result)
    }

    @Test
    fun `returns Disconnecting when status is GATT_SUCCESS and state is STATE_DISCONNECTING`() {
        val result = BluetoothGatt.STATE_DISCONNECTING.toConnectionState(
            status = BluetoothGatt.GATT_SUCCESS,
            bondState = 0
        )
        assertEquals(ConnectionState.Disconnecting, result)
    }

    @Test
    fun `returns Disconnected with unknown state when status is GATT_SUCCESS`() {
        val unknownState = -999
        val result = unknownState.toConnectionState(
            status = BluetoothGatt.GATT_SUCCESS,
            bondState = 0
        )
        assertEquals(ConnectionState.Disconnected("${BluetoothGatt.GATT_SUCCESS}", true), result)
    }

    @Test
    fun `returns Disconnected when status is not GATT_SUCCESS`() {
        val errorStatus = 133
        val result = BluetoothGatt.STATE_CONNECTED.toConnectionState(
            status = errorStatus,
            bondState = 0
        )
        assertEquals(ConnectionState.Disconnected("$errorStatus", false), result)
    }
}
