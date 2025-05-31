package com.example.augmentedrealityglasses.ble.device

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.ConcurrentLinkedQueue

// private data class containing all information related to the connection
data class DeviceConnectionState(
    internal val gatt: BluetoothGatt?,
    val connectionState: Int,
    val service: BluetoothGattService? = null,
    val messageSent: Boolean = false, // state of the last message
    val messageReceived: String = "",
) {
    companion object {
        val None = DeviceConnectionState(null, -1)
    }
}

enum class State {
    ToSend,
    Pending
}

data class Message(
    val data: ByteArray,
    val gatt: BluetoothGatt,
    val characteristic: BluetoothGattCharacteristic,
    var state: State = State.ToSend,
    var nTries: Int = 0,
)

@SuppressLint("MissingPermission")
class BleDevice(
    private val _device: BluetoothDevice,
) : RemoteDevice {
    private var _deviceConnectionState: DeviceConnectionState = DeviceConnectionState.None
    private val TAG = "BleDevice"
    private val characteristicMaxChars = 13

    // use a queue to have a single separate thread to manage ble events
    val taskQueue = ConcurrentLinkedQueue<Runnable>()
    private val worker = Thread {
        while (true) {
            try {
                taskQueue.poll()?.run()
            } catch (e: InterruptedException) {
                Log.d(TAG, "Worker thread interrupted. Exiting.")
                break
            }
            Thread.sleep(500)
        }
    }

    val msgQueue = ConcurrentLinkedQueue<Message>()
    private val msgDispatcher = Thread {
        while (true) {
            try {
                val m = msgQueue.peek()
                if (m != null && m.state == State.ToSend) {
                    Log.d(TAG, "Trying sending new message")
                    val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        m.gatt.writeCharacteristic(
                            m.characteristic,
                            m.data,
                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        )
                    else {
                        m.characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        m.characteristic.value = m.data
                        m.gatt.writeCharacteristic(
                            m.characteristic
                        )
                    }

                    if (status == BluetoothStatusCodes.SUCCESS) {
                        m.state = State.Pending
                    } else {
                        m.state = State.ToSend
                        m.nTries += 1
                        if (m.nTries >= 15) {
                            Log.d(TAG, "Message $m is continuously failing")
                        }
                    }
                }
            } catch (e: InterruptedException) {
                Log.d(TAG, "Message dispatcher interrupted. Exiting.")
                break
            }
            Thread.sleep(500)
        }
    }


    init {
        worker.start()
        msgDispatcher.start()
    }

    private fun handleFlowTransmissionError(operation: String) {
        Log.e(TAG, "Failing in transmitting $operation down the stream")
    }

    private fun initializeConnection(context: Context): Flow<DeviceConnectionState> {
        return callbackFlow {
            val gattCallback = object : BluetoothGattCallback() {
                override fun onConnectionStateChange(
                    gatt: BluetoothGatt,
                    status: Int,
                    newState: Int,
                ) {
                    taskQueue.add {
                        _deviceConnectionState =
                            _deviceConnectionState.copy(gatt = gatt, connectionState = newState)

                        var sendUpdate = true

                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            if (newState == BluetoothGatt.STATE_CONNECTED) {
                                Log.d(TAG, "Connection has been established :)")
                                Log.d(TAG, "Discover services")
                                _deviceConnectionState.gatt?.discoverServices()
                            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                                Log.d(TAG, "Cleaning stuff up")
                                gatt.close()
                                _deviceConnectionState = DeviceConnectionState.None
                                taskQueue.clear()
                                msgQueue.clear()
                                sendUpdate = false
                            } else {
                                Log.d(TAG, "Connecting or disconnecting")
                            }
                        } else {
                            // TODO
                            // Here you should handle the error returned in status based on the constants
                            // https://developer.android.com/reference/android/bluetooth/BluetoothGatt#summary
                            // For example for GATT_INSUFFICIENT_ENCRYPTION or
                            // GATT_INSUFFICIENT_AUTHENTICATION you should create a bond.
                            // https://developer.android.com/reference/android/bluetooth/BluetoothDevice#createBond()
                            Log.d(TAG, "An error happened: $status")
                        }

                        if (sendUpdate) {
                            trySendBlocking(_deviceConnectionState)
                                .onFailure {
                                    handleFlowTransmissionError("connection state")
                                }
                        }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    taskQueue.add {
                        super.onServicesDiscovered(gatt, status)
                        val service = gatt.services.find { it.uuid == SERVICE_UUID }
                        if (service == null) {
                            Log.e(TAG, "Service discovery for ${SERVICE_UUID} failed")
                            disconnect()
                        } else {
                            Log.d(TAG, "Service found")
                            Log.d(TAG, "Setting the characteristic notification to true")
                            enableNotifications(
                                gatt,
                                service.getCharacteristic(CHARACTERISTIC_UUID_RX)
                            )

                            _deviceConnectionState = _deviceConnectionState.copy(service = service)
                            trySendBlocking(_deviceConnectionState)
                                .onFailure {
                                    handleFlowTransmissionError("service discovery")
                                }
                        }
                    }
                }

                override fun onCharacteristicWrite(
                    gatt: BluetoothGatt?,
                    characteristic: BluetoothGattCharacteristic?,
                    status: Int,
                ) {
                    taskQueue.add {
                        super.onCharacteristicWrite(gatt, characteristic, status)
                        Log.d(TAG, "Characteristic $characteristic successfully written")
                        msgQueue.poll()
                        _deviceConnectionState =
                            _deviceConnectionState.copy(messageSent = status == BluetoothGatt.GATT_SUCCESS)
                        trySendBlocking(_deviceConnectionState)
                            .onFailure {
                                handleFlowTransmissionError("characteristic written")
                            }
                    }
                }

                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    value: ByteArray
                ) {
                    taskQueue.add {
                        super.onCharacteristicChanged(gatt, characteristic, value)
                        Log.d(TAG, "Characteristic $characteristic changed")
                        doOnRead(value)
                    }
                }

                @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic
                ) {
                    // Copy the byte array so we have a threadsafe copy
                    val value = ByteArray(characteristic.value.size)
                    characteristic.value.copyInto(value)
                    taskQueue.add {
                        super.onCharacteristicChanged(gatt, characteristic)
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            doOnRead(value)
                        }
                    }
                }

                private fun enableNotifications(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    useAck: Boolean = true
                ) {
                    gatt.setCharacteristicNotification(characteristic, true)
                    val descriptor = characteristic.getDescriptor(DESCRIPTOR_UUID)
                    val value =
                        if (useAck) BluetoothGattDescriptor.ENABLE_INDICATION_VALUE else BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        descriptor.value = value
                        gatt.writeDescriptor(descriptor)
                    } else {
                        gatt.writeDescriptor(descriptor, value)
                    }
                }

                private fun doOnRead(value: ByteArray) {
                    _deviceConnectionState =
                        _deviceConnectionState.copy(messageReceived = value.decodeToString())
                    trySendBlocking(_deviceConnectionState)
                        .onFailure {
                            handleFlowTransmissionError("reading")
                        }
                }
            }

            taskQueue.add {
                Log.d(TAG, "Connecting to the gatt server")
                val gatt = _device.connectGatt(context, false, gattCallback, TRANSPORT_LE)
                _deviceConnectionState = _deviceConnectionState.copy(gatt = gatt)
            }

            awaitClose {
                Log.d(TAG, "Flow has been closed")
                taskQueue.add {
                    Log.d(TAG, "Closing connection")
                    _deviceConnectionState.gatt!!.disconnect()
                }
            }
        }
    }

    // entry point to establish a connection and prepare the flow to expose the information regarding the connection
    override fun connect(context: Context): Flow<DeviceConnectionState> {
        return initializeConnection(context)
    }

    override fun restoreConnection() {
        taskQueue.add {
            require(_deviceConnectionState.gatt != null)
            _deviceConnectionState.gatt?.connect()
        }
    }

    override fun disconnect() {
        taskQueue.add {
            _deviceConnectionState.gatt?.disconnect() ?: Log.d(TAG, "Gatt already disconnected")
        }
    }

    override fun send(msg: String) {
        taskQueue.add {
            val toSend = msg.chunked(characteristicMaxChars).map { m -> m.toByteArray() }
            val characteristic =
                _deviceConnectionState.service!!.getCharacteristic(CHARACTERISTIC_UUID_TX)
            _deviceConnectionState = _deviceConnectionState.copy(messageSent = false)
            if (characteristic != null) {
                for (data in toSend) {
                    _deviceConnectionState.gatt?.let {
                        msgQueue.add(Message(data, it, characteristic))
                    }
                }
            } else {
                Log.d(TAG, "Invalid send operation: the wanted characteristic is not available")
            }
        }
    }
}
