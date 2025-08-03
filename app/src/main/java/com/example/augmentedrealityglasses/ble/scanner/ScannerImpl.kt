package com.example.augmentedrealityglasses.ble.scanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.augmentedrealityglasses.ble.checkOtherwiseExec
import com.example.augmentedrealityglasses.ble.peripheral.Peripheral
import com.example.augmentedrealityglasses.ble.peripheral.PeripheralImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlin.time.Duration

class ScannerImpl(
    private val adapter: BluetoothAdapter,
    private val context: Context,
    private val scanCallback: ScanCallback = ScannerCallbackImpl(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val settings: ScanSettings = ScanSettings.Builder()
        .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .build()
) : Scanner {
    private val _isScanning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isScanning: SharedFlow<Boolean> = _isScanning

    private var job: Job? = null

    private val _scannedPeripherals: MutableSharedFlow<Peripheral> = MutableSharedFlow(10)
    override val scannedPeripherals: SharedFlow<Peripheral> = _scannedPeripherals.asSharedFlow()

    override fun startScanning(
        timeout: Duration,
        filters: List<ScanFilter>
    ): Flow<Peripheral> {
        checkScanningPermission()

        if (_isScanning.value)
            return scannedPeripherals

        job = scope.launch {
            delay(timeout)
            stopScanning()
        }

        scannedPeripherals = scanCallback.scanEvent
            .onSubscription {
                _isScanning.tryEmit(true)
                adapter.bluetoothLeScanner.startScan(filters, settings, scanCallback)
            }
            .takeWhile { it is ScanSuccess }
            .filterIsInstance<ScanSuccess>()
            .map {
                val device = it.scanResult.device
                PeripheralImpl(
                    device,
                    device.address,
                    adapter.bondedDevices.contains(device),
                    context
                )
            }

        return scannedPeripherals
    }

    override fun stopScanning() {
        if (!_isScanning.value)
            return

        job?.cancel()

        checkScanningPermission()

        _isScanning.tryEmit(false)

        adapter.bluetoothLeScanner.stopScan(scanCallback)
    }

    private fun checkScanningPermission() {
        checkOtherwiseExec(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("BLUETOOTH_SCAN permission not granted")
        }
    }
}