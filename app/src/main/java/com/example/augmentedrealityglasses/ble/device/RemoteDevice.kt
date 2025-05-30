package com.example.augmentedrealityglasses.ble.device

import android.content.Context
import kotlinx.coroutines.flow.Flow

interface RemoteDevice {
    /**
     * Connects the device passing the context information.
     * @property context The context of this app
     * @return the flow through which information on the connection state are shared externally
     */
    fun connect(context: Context): Flow<DeviceConnectionState>

    /**
     * Restore the connection previously established. The method differs from connect because
     * it does not create a new connection
     */
    fun restoreConnection()

    /**
     * Sends the message to the remote device.
     * @property msg The message to send to the remote device
     */
    fun send(msg: String)

    /**
     * Disconnects the application from this device.
     */
    fun disconnect()

    /*
    /**
     * Closes the bluetooth gatt client on this device. In contrast with disconnect(), this method forgets
     * all information about the device to which the application was connected to hence requiring the communication
     * to start over.
     */
    fun close()
     */
}