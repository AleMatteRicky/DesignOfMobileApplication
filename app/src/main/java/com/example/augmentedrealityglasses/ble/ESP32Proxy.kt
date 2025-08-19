package com.example.augmentedrealityglasses.ble

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.example.augmentedrealityglasses.ble.devicedata.CHARACTERISTIC_UUID_RX
import com.example.augmentedrealityglasses.ble.devicedata.CHARACTERISTIC_UUID_TX
import com.example.augmentedrealityglasses.ble.devicedata.RemoteDeviceData
import com.example.augmentedrealityglasses.ble.devicedata.RemoteDeviceManager
import com.example.augmentedrealityglasses.ble.devicedata.SERVICE_UUID
import com.example.augmentedrealityglasses.ble.manager.BluetoothManager
import com.example.augmentedrealityglasses.ble.manager.BluetoothManagerImpl
import com.example.augmentedrealityglasses.ble.manager.BluetoothState
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ESP32Proxy(
    val context: Context,
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) :
    RemoteDeviceManager {

    companion object {
        private val TAG = ESP32Proxy::class.simpleName
        val ESP32MAC: String = "20:43:A8:6A:ED:2A"
    }

    private val bluetoothManager: BluetoothManager = BluetoothManagerImpl(context, scope)

    override val bluetoothState: StateFlow<BluetoothState> = bluetoothManager.bluetoothOn

    private val _peripheralConnectionState: MutableStateFlow<RemoteDeviceData> =
        MutableStateFlow(RemoteDeviceData.None)
    val peripheralConnectionState: StateFlow<RemoteDeviceData> =
        _peripheralConnectionState.asStateFlow()

    private var _device: BluetoothDevice? = null

    private val jobs: MutableList<Job> = mutableListOf()

    private var isDeviceSet = false

    override fun connect() {
        require(_device != null) {
            "Device not yet set despite the request to use it"
        }

        /* this request is triggered by the UI when the device is disconnected for external causes */
        bluetoothManager.createStub(_device!!).connect()
    }

    override fun setDeviceToManage(device: BluetoothDevice) {
        val isConnectedToTheExpectedDevice = device.address == ESP32MAC
        require(isConnectedToTheExpectedDevice) {
            "Expected MAC: ${ESP32MAC}, received: ${device.address} device passed!!!"
        }

        _device = device
        isDeviceSet = true
    }

    override fun isDeviceSet(): Boolean {
        return isDeviceSet
    }

    override fun isConnected(): Boolean {
        if (!isDeviceSet()) {
            return false
        } else {
            val stub = bluetoothManager.getStub(ESP32MAC)
            if (stub == null) {
                return false
            } else {
                return stub.connectionState.value is ConnectionState.Connected
            }
        }
    }

    /*
    Listen for updates coming from the device. Every time a new connection is established, the function
    is invoked to listen from the new updated version
     */
    private fun listen() {
        // cancel jobs to avoid leaks
        jobs.forEach { it.cancel() }
        jobs.clear()

        val peripheral = bluetoothManager.getStub(ESP32MAC)!!

        jobs.add(scope.launch {
            peripheral.connectionState
                .collect { state ->
                    _peripheralConnectionState.update {
                        it.copy(connectionState = state)
                    }
                }
        }
        )

        jobs.add(
            // receive updates from the peripheral
            scope.launch {
                if (!peripheral.areServicesAvailable.value) {
                    peripheral.discoverServices()
                }

                peripheral.subscribe(SERVICE_UUID, CHARACTERISTIC_UUID_RX)
                    .collect { msg ->
                        _peripheralConnectionState.update {
                            it.copy(messageReceived = msg)
                        }
                    }
            }
        )
    }

    override fun receiveUpdates(): StateFlow<RemoteDeviceData> {
        listen()
        return peripheralConnectionState
    }

    override suspend fun send(msg: String) {
        val peripheral = bluetoothManager.getStub(ESP32MAC)!!
        if (!peripheral.areServicesAvailable.value) {
            peripheral.discoverServices()
        }
        peripheral.send(SERVICE_UUID, CHARACTERISTIC_UUID_TX, msg.toByteArray())
    }

    override fun disconnect() {
        bluetoothManager.getStub(ESP32MAC)?.disconnect()
    }
}