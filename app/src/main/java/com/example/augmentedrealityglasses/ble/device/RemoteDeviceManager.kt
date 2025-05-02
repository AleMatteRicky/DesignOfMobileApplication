package com.example.augmentedrealityglasses.ble.device

import kotlinx.coroutines.flow.Flow

interface RemoteDeviceManager{
    /**
     * Connects the remote device
     */
    fun connect()

    /**
     * Sets the remote device that will be managed by this instance.
     */
    fun setDeviceToManage(device: RemoteDevice)

    /**
     * Starts receiving updates from the remote device
     * @return flow to consume the updates
     */
    fun receiveUpdates(): Flow<DeviceConnectionState>

     /**
     * @return true if the remote device is connected, false otherwise
     */
    fun isConnected() : Boolean

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

    /**
     * Closes the bluetooth gatt client on the device. In contrast with disconnect(), this method forgets
     * all information about the device to which the application was connected to hence requiring the communication
     * to start over.
     */
    fun close()
}