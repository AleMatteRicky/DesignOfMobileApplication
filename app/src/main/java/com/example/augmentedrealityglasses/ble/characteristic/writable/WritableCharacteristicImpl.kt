package com.example.augmentedrealityglasses.ble.characteristic.writable

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import com.example.augmentedrealityglasses.ble.characteristic.Characteristic
import com.example.augmentedrealityglasses.ble.characteristic.toCharacteristicProperties
import com.example.augmentedrealityglasses.ble.characteristic.writable.dispatcher.MsgDispatcher
import com.example.augmentedrealityglasses.ble.characteristic.writable.dispatcher.MsgDispatcherImpl
import com.example.augmentedrealityglasses.ble.characteristic.writable.message.Message
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.CharacteristicWriteEvent
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.GattEvent
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import java.util.UUID

class WritableCharacteristicImpl(
    override val uuid: UUID,
    override val maximumLength : Int,
    val events: SharedFlow<GattEvent>,
    val gatt: BluetoothGatt,
    val characteristic: BluetoothGattCharacteristic,
    val context: Context,
    val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : WritableCharacteristic {
    private var msgDispatcher : MsgDispatcher = MsgDispatcherImpl(
        context
    )

    override val properties: Set<Characteristic.CharacteristicProperty> =
        characteristic.properties.toCharacteristicProperties()

    override suspend fun write(value: ByteArray) {
        val n = value.size
        for (i in 0 until n step maximumLength) {
            val data = value.sliceArray(i until minOf(i + maximumLength, n))
            msgDispatcher.add(Message(data, gatt, characteristic))
        }
    }

    suspend fun cleanup() {
        msgDispatcher.close()
        scope.cancel("Characteristic has finished writing")
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