package com.example.augmentedrealityglasses.ble.viewmodels

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.augmentedrealityglasses.App
import com.example.augmentedrealityglasses.ble.devicedata.RemoteDeviceManager
import com.example.augmentedrealityglasses.ble.scanner.ScanError
import com.example.augmentedrealityglasses.ble.scanner.ScanSuccess
import com.example.augmentedrealityglasses.ble.scanner.Scanner
import com.example.augmentedrealityglasses.ble.scanner.ScannerImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class FindDeviceViewModel(
    private val bleManager: RemoteDeviceManager,
    private val scanner: Scanner
) : ViewModel() {
    private val _isScanning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // use of a list to maintain the order of scanned devices
    private val _scannedDevices: MutableStateFlow<List<BluetoothDevice>> =
        MutableStateFlow(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDevice>> = _scannedDevices.asStateFlow()
    private var getScannedDevicesJob: Job? = null

    fun connect(device: BluetoothDevice) {
        bleManager.setDeviceToManage(device)
        bleManager.connect()
    }

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

    companion object {
        val ADAPTER_KEY = object : CreationExtras.Key<BluetoothAdapter> {}

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val adapter: BluetoothAdapter = this[ADAPTER_KEY] as BluetoothAdapter
                val app = this[APPLICATION_KEY] as App
                FindDeviceViewModel(
                    bleManager = app.container.proxy,
                    scanner = ScannerImpl(adapter, app)
                )
            }
        }

        private val TAG = FindDeviceViewModel::class.simpleName
    }

    override fun onCleared() {
        Log.d(TAG, "Virtual view cleared")
    }

}