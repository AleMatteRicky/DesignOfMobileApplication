package com.example.augmentedrealityglasses.ble.characteristic.writable

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import com.example.augmentedrealityglasses.ble.GattOperationMutex
import com.example.augmentedrealityglasses.ble.characteristic.Characteristic
import com.example.augmentedrealityglasses.ble.characteristic.writable.dispatcher.MsgDispatcherImpl
import com.example.augmentedrealityglasses.ble.characteristic.writable.message.Message
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.CharacteristicWriteEvent
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.GattEvent
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import java.util.UUID

class WritableCharacteristicImpl(
    override val uuid: UUID,
    val events: SharedFlow<GattEvent>,
    val gatt: BluetoothGatt,
    val characteristic: BluetoothGattCharacteristic,
    scope: CoroutineScope,
    val context: Context,
) : WritableCharacteristic {
    private val maxSz = 13

    private val msgDispatcher =
        MsgDispatcherImpl(
            context
        )

    override val properties: Set<Characteristic.CharacteristicProperty> =
        setOf(Characteristic.CharacteristicProperty.WRITE)

    override suspend fun write(value: ByteArray) {
        // mutex used to have the messages sent in order
        return GattOperationMutex.withLock {
            val n = value.size
            for (i in 0 until n step maxSz) {
                val data = value.sliceArray(i until minOf(i + maxSz, n))
                msgDispatcher.add(Message(data, gatt, characteristic))
            }
        }
    }

    suspend fun cleanup() {
        msgDispatcher.close()
    }

    init {
        scope.launch {
            events
                .takeWhile { !it.isServiceInvalidatedEvent }
                .filterIsInstance<CharacteristicWriteEvent>()
                .filter {
                    it.characteristic.uuid == uuid
                            &&
                            it.status == Status.SUCCESS
                }
                .collect {
                    msgDispatcher.removeFirst()
                }
            cleanup()
        }
    }
}