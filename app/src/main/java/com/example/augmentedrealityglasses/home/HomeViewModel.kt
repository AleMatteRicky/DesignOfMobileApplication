package com.example.augmentedrealityglasses.home

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.augmentedrealityglasses.container.App
import com.example.augmentedrealityglasses.update.BluetoothUpdateStatus
import com.example.augmentedrealityglasses.ble.ESP32Proxy
import com.example.augmentedrealityglasses.ble.devicedata.RemoteDeviceManager
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.ConnectionState
import com.example.augmentedrealityglasses.ble.scanner.ScanError
import com.example.augmentedrealityglasses.ble.scanner.ScanSuccess
import com.example.augmentedrealityglasses.ble.scanner.Scanner
import com.example.augmentedrealityglasses.ble.scanner.ScannerImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class HomeViewModel(
    private val proxy: RemoteDeviceManager,
    private val scanner: Scanner
) : ViewModel() {

    var bluetoothUpdateStatus by mutableStateOf(BluetoothUpdateStatus.NONE)
        private set

    //Initialize the viewModel
    companion object {
        val ADAPTER_KEY = object : CreationExtras.Key<BluetoothAdapter> {}

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val adapter: BluetoothAdapter = this[ADAPTER_KEY] as BluetoothAdapter
                val app = this[APPLICATION_KEY] as App
                HomeViewModel(
                    proxy = app.container.proxy,
                    scanner = ScannerImpl(adapter, app)
                )
            }
        }

        private val TAG = HomeViewModel::class.simpleName
    }

    // Tracks the Bluetooth connection status with the external device
    var isExtDeviceConnected by mutableStateOf(false)
        private set

    // Start listening for Bluetooth packets
    init {
        viewModelScope.launch {
            if (proxy.isConnected()) {
                proxy.receiveUpdates()
                    .collect { connectionState ->
                        if (connectionState.connectionState is ConnectionState.Connected) {
                            isExtDeviceConnected = true
                            bluetoothUpdateStatus = BluetoothUpdateStatus.DEVICE_CONNECTED
                        } else {
                            isExtDeviceConnected = false
                            bluetoothUpdateStatus = BluetoothUpdateStatus.DEVICE_DISCONNECTED
                        }
                    }
            }
        }
    }

    var bondedDevices: List<BluetoothDevice> by mutableStateOf(emptyList())

    private val _isScanning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // use of a list to maintain the order of scanned devices
    private val _scannedDevices: MutableStateFlow<List<BluetoothDevice>> =
        MutableStateFlow(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDevice>> = _scannedDevices.asStateFlow()
    private var getScannedDevicesJob: Job? = null

    var errorMessage by mutableStateOf("")

    fun hideErrorMessage() {
        errorMessage = ""
    }

    fun hideBluetoothUpdate() {
        bluetoothUpdateStatus = BluetoothUpdateStatus.NONE
    }

    private fun showErrorMessage(msg: String) {
        errorMessage = msg
    }

    fun disconnectDevice() {
        //FIXME: switch isExtDeviceConnected flag to false
        proxy.disconnect()
    }

    fun refreshBondedDevices(context: Context, adapter: BluetoothAdapter?) {
        if (ContextCompat.checkSelfPermission(context, BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            bondedDevices = emptyList()
        } else {
            bondedDevices = adapter?.bondedDevices.orEmpty().toList()
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice): Boolean {
        if (device.address != ESP32Proxy.ESP32MAC) {
            showErrorMessage("Device not supported")
            return false
        }

        proxy.setDeviceToManage(device)
        proxy.connect()

        viewModelScope.launch {
            proxy.receiveUpdates()
                .collect { connectionState ->
                    if (connectionState.connectionState is ConnectionState.Connected) {
                        isExtDeviceConnected = true
                        bluetoothUpdateStatus = BluetoothUpdateStatus.DEVICE_CONNECTED
                    } else {
                        isExtDeviceConnected = false
                        bluetoothUpdateStatus = BluetoothUpdateStatus.DEVICE_DISCONNECTED
                    }
                }
        }

        return true
    }

    @SuppressLint("MissingPermission")
    fun tryToConnectBondedDevice(
        device: BluetoothDevice,
        timeout: Duration = 5.seconds
    ) {
        viewModelScope.launch {
            if (device.address != ESP32Proxy.ESP32MAC) {
                showErrorMessage("Invalid device")
                return@launch
            }
            if (device.bondState != BluetoothDevice.BOND_BONDED) {
                return@launch
            }

            val filters = listOf(
                ScanFilter.Builder()
                    .setDeviceAddress(device.address)
                    .build()
            )

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            scan(timeout, filters, settings)

            val found = withTimeoutOrNull(timeout) {
                scannedDevices
                    .map { list ->
                        list.any {
                            it.address.equals(
                                device.address,
                                ignoreCase = true
                            )
                        }
                    }
                    .first { it }
            } ?: false

            if (!found) {
                showErrorMessage("Unavailable device")
                return@launch
            }

            if (!connect(device)) {
                showErrorMessage("Connection error")
            }
        }
    }

    //FIXME
    @SuppressLint("MissingPermission")
    fun scan(
        timeout: Duration = 10.seconds,
        filters: List<ScanFilter>?,
        settings: ScanSettings
    ) {
        // clear all devices previously emitted
        _scannedDevices.tryEmit(emptyList())
        getScannedDevicesJob?.cancel()
        _isScanning.tryEmit(true)

        getScannedDevicesJob = viewModelScope.launch {
            scanner.scan(timeout, filters, settings)
                .takeWhile { it !is ScanError }
                .filterIsInstance<ScanSuccess>()
                .map {
                    it.scanResult.device
                }
                .filter {
                    it.name != null
                }
                .collect { device ->
                    _scannedDevices.update {
                        if (it.contains(device)) {
                            it
                        } else {
                            it + setOf(device)
                        }
                    }
                }

            _isScanning.tryEmit(false)
        }
    }

    fun stopScanning() {
        getScannedDevicesJob?.cancel()
        _isScanning.tryEmit(false)
    }

    override fun onCleared() {
        Log.d(TAG, "Virtual view cleared")
    }
}