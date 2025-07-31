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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlin.time.Duration

class ScannerImpl(
    private val adapter: BluetoothAdapter,
    private val scanCallback: ScannerCallbackImpl = ScannerCallbackImpl(),
    private val context: Context,
    private val scope: CoroutineScope,
    private val settings: ScanSettings = ScanSettings.Builder()
        .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .build()
) : Scanner {
    private val _isScanning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isScanning: SharedFlow<Boolean> = _isScanning

    private var job: Job? = null

    override fun startScanning(
        timeout: Duration,
        filters: List<ScanFilter>
    ): SharedFlow<ScanEvent> {
        checkScanningPermission()

        if (_isScanning.value)
            return scanCallback.scanEvent

        job = scope.launch {
            delay(timeout)
            stopScanning()
        }

        return scanCallback.scanEvent.onSubscription {
            _isScanning.tryEmit(true)
            adapter.bluetoothLeScanner.startScan(filters, settings, scanCallback)
        }
            .map {
                if (it is ScanSuccess) {
                    ScanSuccess(
                        it.scanResult,
                        adapter.bondedDevices.contains(it.scanResult!!.device)
                    )
                } else {
                    it
                }
            } as SharedFlow<ScanEvent>
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