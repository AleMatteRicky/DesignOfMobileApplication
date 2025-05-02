package com.example.augmentedrealityglasses.ble.device

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// private data class containing all information related to the connection
data class DeviceConnectionState(
    internal val gatt: BluetoothGatt?,
    val connectionState: Int,
    val mtu: Int,
    //val services: List<BluetoothGattService> = emptyList(),
    val service: BluetoothGattService? = null,
    val messageSent: Boolean = false, // state of the last message
    val messageReceived: String = "",
) {
    companion object {
        val None = DeviceConnectionState(null, -1, -1)
    }
}

/*
TODO: add callback and other functions to send and receive updates
TODO: add the callback onCharacteristicChanged to be notified about changes in the characteristic. This requires calling setCharacteristicNotification(characteristic,enabled)
*/

@SuppressLint("MissingPermission")
class BleDevice(
    private val _device: BluetoothDevice,
) : RemoteDevice {
    private var _deviceConnectionState: DeviceConnectionState = DeviceConnectionState.None
    private val TAG = "BleDevice"
    private var service: BluetoothGattService? = null

    private fun handleFlowTransmissionError(operation: String) {
        Log.d(TAG, "Failing in transmitting $operation down the stream")
    }

    private fun initializeConnection(context: Context): Flow<DeviceConnectionState> {
        return callbackFlow {
            val gattCallback = object : BluetoothGattCallback() {
                override fun onConnectionStateChange(
                    gatt: BluetoothGatt,
                    status: Int,
                    newState: Int,
                ) {
                    super.onConnectionStateChange(gatt, status, newState)
                    _deviceConnectionState =
                        _deviceConnectionState.copy(gatt = gatt, connectionState = newState)

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        if (newState == BluetoothGatt.STATE_CONNECTED) {
                            Log.d(TAG, "Connection has been established :)")
                            Log.d(TAG, "Discover services")
                            _deviceConnectionState.gatt?.discoverServices()
                        } else {
                            Log.d(TAG, "Disconnection")
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

                    trySendBlocking(_deviceConnectionState)
                        .onFailure {
                            handleFlowTransmissionError("connection state")
                        }
                }

                override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                    super.onMtuChanged(gatt, mtu, status)
                    _deviceConnectionState = _deviceConnectionState.copy(gatt = gatt, mtu = mtu)
                    trySendBlocking(_deviceConnectionState)
                        .onFailure {
                            handleFlowTransmissionError("mtu")
                        }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    super.onServicesDiscovered(gatt, status)
                    Log.d(TAG, "Services discovered: ${gatt.services}")
                    val service = gatt.services.find { it.uuid == SERVICE_UUID }
                    if (service == null) {
                        Log.d(TAG, "Service NOT FOUND!!!")
                        // TODO. it might require code for handling this situation. For now keep it null, it won't trigger any recomposition
                    } else {
                        Log.d(TAG, "Service found")
                        Log.d(TAG, "Setting the characteristic notification to true")
                        enableNotifications(gatt, service.getCharacteristic(CHARACTERISTIC_UUID))
                    }


                    _deviceConnectionState = _deviceConnectionState.copy(service = service)
                    trySendBlocking(_deviceConnectionState)
                        .onFailure {
                            handleFlowTransmissionError("service discovery")
                        }
                }

                override fun onCharacteristicWrite(
                    gatt: BluetoothGatt?,
                    characteristic: BluetoothGattCharacteristic?,
                    status: Int,
                ) {
                    super.onCharacteristicWrite(gatt, characteristic, status)
                    Log.d(TAG, "Characteristic $characteristic successfully written")
                    _deviceConnectionState =
                        _deviceConnectionState.copy(messageSent = status == BluetoothGatt.GATT_SUCCESS)
                    trySendBlocking(_deviceConnectionState)
                        .onFailure {
                            handleFlowTransmissionError("characteristic written")
                        }
                }

                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    value: ByteArray
                ) {
                    super.onCharacteristicChanged(gatt, characteristic, value)
                    Log.d(TAG, "Characteristic $characteristic changed")
                    doOnRead(value)
                }

                @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt?,
                    characteristic: BluetoothGattCharacteristic?
                ) {
                    super.onCharacteristicChanged(gatt, characteristic)
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        if (characteristic != null) {
                            doOnRead(characteristic.value)
                        }
                    }
                }

                @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                override fun onCharacteristicRead(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    status: Int,
                ) {
                    super.onCharacteristicRead(gatt, characteristic, status)
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        doOnRead(characteristic.value)
                    }
                }

                override fun onCharacteristicRead(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    value: ByteArray,
                    status: Int,
                ) {
                    super.onCharacteristicRead(gatt, characteristic, value, status)
                    doOnRead(value)
                }

                private fun enableNotifications(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic
                ) {
                    gatt.setCharacteristicNotification(characteristic, true)

                    /*
                    val CLIENT_CHARACTERISTIC_CONFIG_UUID : UUID = UUID.fromString("00002902–0000–1000–8000–00805f9b34fb")
                    val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)  {
                        gatt.writeDescriptor(descriptor)
                    } else {
                        gatt.writeDescriptor(descriptor, descriptor.value)
                    }
                     */
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

            Log.d(TAG, "Connecting to the gatt server")
            val gatt = _device.connectGatt(context, false, gattCallback)

            _deviceConnectionState = _deviceConnectionState.copy(gatt = gatt)

            awaitClose {
                Log.d(TAG, "Flow has been closed")
                _deviceConnectionState.gatt?.close()
                _deviceConnectionState = DeviceConnectionState.None
            }
        }
    }

    // entry point to establish a connection and prepare the flow to exposes the information regarding the connection
    override fun connect(context: Context): Flow<DeviceConnectionState> {
        return initializeConnection(context)
    }

    override fun restoreConnection() {
        require(_deviceConnectionState.gatt != null)
        _deviceConnectionState.gatt?.connect()
    }

    override fun disconnect() {
        _deviceConnectionState.gatt?.disconnect() ?: Log.d(TAG, "Gatt already disconnected")
    }

    // TODO. If the gatt is busy, the writeCharacteristic function might fail, hence we might need extra code to handle this case
    override fun send(msg: String) {
        val data = msg.toByteArray()
        val characteristic = _deviceConnectionState.service?.getCharacteristic(CHARACTERISTIC_UUID)
        _deviceConnectionState = _deviceConnectionState.copy(messageSent = false)
        if (characteristic != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                _deviceConnectionState.gatt?.writeCharacteristic(
                    characteristic,
                    data,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                )
            } else {
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                characteristic.value = data
                @Suppress("DEPRECATION")
                _deviceConnectionState.gatt?.writeCharacteristic(characteristic)
            }
        } else {
            Log.d(TAG, "Invalid send operation: the wanted characteristic is not available")
        }
    }

    override fun close() {
        _deviceConnectionState.gatt?.close() ?: Log.d(TAG, "Connection already closed")
    }

    override fun isConnected() : Boolean {
        return _deviceConnectionState.gatt != null
    }
}
