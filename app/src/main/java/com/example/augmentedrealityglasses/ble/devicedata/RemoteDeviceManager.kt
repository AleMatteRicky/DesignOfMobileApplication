package com.example.augmentedrealityglasses.ble.devicedata

import android.bluetooth.BluetoothDevice
import com.example.augmentedrealityglasses.ble.manager.BluetoothState
import kotlinx.coroutines.flow.StateFlow

interface RemoteDeviceManager{
    val bluetoothState : StateFlow<BluetoothState>

    /**
     * Connects the remote device
     */
    fun connect()

    /**
     * Sets the remote device that will be managed by this instance.
     */
    fun setDeviceToManage(device: BluetoothDevice)

    /**
     * After a connection has been established, starts receiving updates from the remote device
     * @return a shared flow to consume the updates
     */
    fun receiveUpdates(): StateFlow<RemoteDeviceData>

    /**
     * Sends the message to the remote device.
     * @property msg The message to send to the remote device
     */
    suspend fun send(msg: String)

    /**
     * Disconnects the application from this device.
     */
    fun disconnect()
}