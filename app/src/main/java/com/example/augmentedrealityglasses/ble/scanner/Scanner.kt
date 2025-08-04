package com.example.augmentedrealityglasses.ble.scanner

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanFilter
import kotlinx.coroutines.flow.SharedFlow
import kotlin.time.Duration

interface Scanner {
    fun startScanning(timeout: Duration, filters : List<ScanFilter>)
    fun stopScanning()
    val scannedDevices : SharedFlow<BluetoothDevice>
    val isScanning : SharedFlow<Boolean>
}