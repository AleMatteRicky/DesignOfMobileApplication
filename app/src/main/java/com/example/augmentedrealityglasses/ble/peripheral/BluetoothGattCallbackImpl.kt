package com.example.augmentedrealityglasses.ble.peripheral

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import androidx.annotation.RequiresPermission
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.CharacteristicChangedEvent
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.CharacteristicWriteEvent
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.ConnectionEvent
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.DescriptorWriteEvent
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.GattEvent
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.ServiceChangedEvent
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.ServiceDiscoveredEvent
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.Status
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.genStatus
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.toConnectionState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class BluetoothGattCallbackImpl(
    val device: BluetoothDevice,
) : BluetoothGattCallback() {
    private val _events: MutableSharedFlow<GattEvent> =
        MutableSharedFlow(replay = 5, extraBufferCapacity = 128)
    override val events: SharedFlow<GattEvent> = _events

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onConnectionStateChange(
        gatt: BluetoothGatt,
        status: Int,
        newState: Int,
    ) {
        val bondState = device.bondState
        _events.tryEmit(ConnectionEvent(newState.toConnectionState(status, bondState)))
    }

    override fun onServiceChanged(gatt: BluetoothGatt) {
        _events.tryEmit(ServiceChangedEvent)
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        val state: Status = status.genStatus()
        if (state == Status.SUCCESS) {
            _events.tryEmit(ServiceDiscoveredEvent(gatt.services))
        } else {
            // calling gatt.services would be an error in this situation
            _events.tryEmit(ServiceDiscoveredEvent(null))
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int,
    ) {
        _events.tryEmit(CharacteristicWriteEvent(status.genStatus(), characteristic))
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        _events.tryEmit(CharacteristicChangedEvent(characteristic, value))
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        // Copy the byte array so we have a threadsafe copy
        val value = ByteArray(characteristic.value.size)
        characteristic.value.copyInto(value)
        _events.tryEmit(CharacteristicChangedEvent(characteristic, value))
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int
    ) {
        _events.tryEmit(DescriptorWriteEvent(descriptor, status.genStatus()))
    }
}