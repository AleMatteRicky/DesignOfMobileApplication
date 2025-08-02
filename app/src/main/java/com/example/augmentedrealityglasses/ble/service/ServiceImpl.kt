package com.example.augmentedrealityglasses.ble.service

import android.bluetooth.BluetoothGattService
import com.example.augmentedrealityglasses.ble.characteristic.readable.ReadableCharacteristic
import com.example.augmentedrealityglasses.ble.characteristic.writable.WritableCharacteristic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class ServiceImpl(
    service: BluetoothGattService,
    override val writableCharacteristics: List<WritableCharacteristic>,
    override val readableCharacteristics: List<ReadableCharacteristic>,
) : Service {
    override val uuid: UUID = service.uuid

    private val _isAvailable: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val isAvailable: StateFlow<Boolean> = _isAvailable

    override fun stopService() {
        _isAvailable.tryEmit(false)
    }
}