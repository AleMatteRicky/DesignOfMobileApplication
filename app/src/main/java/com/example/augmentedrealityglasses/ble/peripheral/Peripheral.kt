package com.example.augmentedrealityglasses.ble.peripheral

import com.example.augmentedrealityglasses.ble.peripheral.bonding.BondState
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.ConnectionState
import com.example.augmentedrealityglasses.ble.service.Service
import kotlinx.coroutines.flow.StateFlow

/**
 * Class representing a peripheral
 */
interface Peripheral {
    val mac : String
    val name : String
    val paired : Boolean
    val connectionState : StateFlow<ConnectionState>
    val bondingState : StateFlow<BondState>
    val services : List<Service>

    /**
     * Disconnects from the peripheral.
     */
    suspend fun disconnect()

    /**
     * Close the peripheral, releasing any resource it was holding
     */
    suspend fun close()

    /**
     * Connects to the peripheral
     */
    suspend fun connect()

    /**
     * Discover services
     */
    suspend fun discoverServices() : List<Service>
}