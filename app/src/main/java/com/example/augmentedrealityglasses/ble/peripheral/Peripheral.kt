package com.example.augmentedrealityglasses.ble.peripheral

import com.example.augmentedrealityglasses.ble.peripheral.bonding.BondState
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.ConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Class representing the stub to communicate with the remote device
 */
interface Peripheral {
    val mac : String
    val name : String?
    val areServicesAvailable : StateFlow<Boolean>
    val connectionState : StateFlow<ConnectionState>
    val bondingState : StateFlow<BondState>
    val services : List<Service>

    /**
     * Disconnects from the peripheral.
     */
    fun disconnect()

    /**
     * Closes the peripheral, releasing any resource it was holding
     */
    fun close()

    /**
     * Connects to the peripheral
     */
    fun connect()

    /**
     * Sends the provided value to the specified service's characteristic
     * @param characteristicUUID of the characteristic to be written
     * @param value to write
     */
    suspend fun send(serviceUUID: UUID, characteristicUUID: UUID, value : ByteArray)

    /**
     * Subscribes to the changes of the specified service's characteristic
     * @param serviceUUID of the characteristic's service
     * @param characteristicUUID of the characteristic to be subscribed to
     * @result flow of string values coming from the characteristic
     */
    suspend fun subscribe(serviceUUID: UUID, characteristicUUID: UUID) : Flow<String>

    /**
     * Discovers the services provided by the peripheral
     */
    suspend fun discoverServices()
}