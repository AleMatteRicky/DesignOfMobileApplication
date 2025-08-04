package com.example.augmentedrealityglasses.ble.characteristic.readable

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.example.augmentedrealityglasses.ble.GattOperationMutex
import com.example.augmentedrealityglasses.ble.InvalidGattOperationException
import com.example.augmentedrealityglasses.ble.characteristic.Characteristic
import com.example.augmentedrealityglasses.ble.characteristic.checkBluetoothConnectPermission
import com.example.augmentedrealityglasses.ble.characteristic.toCharacteristicProperties
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.CharacteristicChangedEvent
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.CharacteristicReadEvent
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.DescriptorWriteEvent
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.GattEvent
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.Status
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.takeWhile
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class ReadableCharacteristicImpl(
    override val uuid: UUID,
    val events: SharedFlow<GattEvent>,
    val gatt: BluetoothGatt,
    val characteristic: BluetoothGattCharacteristic,
    val descriptors: Set<BluetoothGattDescriptor>,
    val context: Context,
) : ReadableCharacteristic {
    override val properties: Set<Characteristic.CharacteristicProperty> =
        characteristic.properties.toCharacteristicProperties()

    private val _isNotifyEnabled = MutableStateFlow(false)
    override val isNotifyEnabled: StateFlow<Boolean> = _isNotifyEnabled.asStateFlow()

    override fun subscribe(): Flow<ByteArray> {
        return events.takeWhile { !it.isServiceInvalidatedEvent }
            .filterIsInstance<CharacteristicChangedEvent>()
            .filter { it.characteristic == characteristic }
            .map { it.value }
    }

    override suspend fun read(): ByteArray? {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        return GattOperationMutex.withLock {
            events.onSubscription {
                gatt.readCharacteristic(characteristic)
            }.takeWhile { !it.isServiceInvalidatedEvent }
                .filterIsInstance<CharacteristicReadEvent>()
                .firstOrNull()
                ?.let {
                    if (it.status == Status.FAILURE) {
                        null
                    } else {
                        it.value
                    }
                }
        }
    }

    override suspend fun setNotify(enable: Boolean): Boolean {
        if (enable
            && !(properties.contains(Characteristic.CharacteristicProperty.NOTIFY)
                    ||
                    properties.contains(Characteristic.CharacteristicProperty.INDICATE))
        )
            throw IllegalArgumentException("This notification cannot support notification/indication")

        val descriptor = descriptors.firstOrNull()

        require(descriptor != null) {
            throw IllegalStateException("the characteristic has no descriptor!!!")
        }

        checkBluetoothConnectPermission(context)

        return GattOperationMutex.withLock {
            val success = events.onSubscription {
                val state = gatt.setCharacteristicNotification(characteristic, enable)
                if (!state) {
                    throw InvalidGattOperationException()
                }
                val value = when {
                    enable && Characteristic.CharacteristicProperty.INDICATE in properties -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                    enable -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    else -> BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    descriptor.value = value
                    gatt.writeDescriptor(descriptor)
                } else {
                    gatt.writeDescriptor(descriptor, value)
                }
            }.takeWhile { !it.isServiceInvalidatedEvent }
                .filterIsInstance<DescriptorWriteEvent>()
                .filter { it.descriptor == descriptor }
                .firstOrNull()

            val res = success != null
            _isNotifyEnabled.emit(res)
            res
        }
    }
}