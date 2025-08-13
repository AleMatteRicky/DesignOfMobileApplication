package com.example.augmentedrealityglasses.ble.service

import android.bluetooth.BluetoothGattService
import com.example.augmentedrealityglasses.ble.characteristic.readable.ReadableCharacteristic
import com.example.augmentedrealityglasses.ble.characteristic.writable.WritableCharacteristic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ServiceImpl(
    service: BluetoothGattService,
    private val writableCharacteristics: List<WritableCharacteristic>,
    private val readableCharacteristics: List<ReadableCharacteristic>,
) : Service {
    override val uuid: UUID = service.uuid

    private val _isAvailable: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    override val writableCharacteristicsUUIDs: List<UUID> = writableCharacteristics.map { it.uuid }
    override val readableCharacteristicsUUIDs: List<UUID> = readableCharacteristics.map { it.uuid }

    override suspend fun writeCharacteristic(
        characteristicUUID: UUID,
        value: ByteArray
    ) {
        if (!_isAvailable.value) {
            throw IllegalStateException("The service is not available")
        }

        val characteristic = writableCharacteristics.find { it.uuid == characteristicUUID }

        if (characteristic == null) {
            throw IllegalArgumentException(
                "The provided UUID is invalid: either the characteristic is not supported by this service or it is not readable"
            )
        }

        characteristic.write(value)
    }

    override suspend fun subscribeCharacteristic(characteristicUUID: UUID): Flow<ByteArray> {

        if (!_isAvailable.value) {
            throw IllegalStateException("The service is not available")
        }

        val characteristic = readableCharacteristics.find { it.uuid == characteristicUUID }

        if (characteristic == null) {
            throw IllegalArgumentException(
                "The provided UUID is invalid: either the characteristic is not supported by this service or it is not readable"
            )
        }

        if(!characteristic.isNotifyEnabled.value) {
            characteristic.setNotify(true)
        }

        return characteristic.subscribe()
    }

    override fun stopService() {
        _isAvailable.tryEmit(false)
    }
}