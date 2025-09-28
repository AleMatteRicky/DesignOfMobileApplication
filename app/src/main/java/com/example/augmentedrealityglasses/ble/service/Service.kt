package com.example.augmentedrealityglasses.ble.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

interface Service {
    /**
     * UUID of the service
     */
    val uuid : UUID

    /**
     * Flow to know whether the service is available or not. If a service is not available is because
     * either the remote device is not connected, or the service itself has not been discovered yet
     */
    val isAvailable : StateFlow<Boolean>

    /**
     * List of UUIDs of writable characteristics supported by the service
     */
    val writableCharacteristicsUUIDs : List<UUID>

    /**
     * List of UUIDs of readable characteristics supported by the service
     */
    val readableCharacteristicsUUIDs : List<UUID>

    /**
     * Writes the specified value to the characteristic
     * @param characteristicUUID UUID of the characteristic to write
     * @param value to be written
     */
    suspend fun writeCharacteristic(characteristicUUID: UUID, value: ByteArray)

    /**
     * Subscribes to the changes of the provided characteristic
     * @param characteristicUUID UUID of the characteristic to be subscribed
     */
    suspend fun subscribeCharacteristic(characteristicUUID: UUID) : Flow<ByteArray>

    /**
     * Stops the service
     */
    fun stopService()
}