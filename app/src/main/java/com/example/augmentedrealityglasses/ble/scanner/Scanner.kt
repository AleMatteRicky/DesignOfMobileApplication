package com.example.augmentedrealityglasses.ble.scanner

import android.bluetooth.le.ScanFilter
import kotlinx.coroutines.flow.SharedFlow
import kotlin.time.Duration

interface Scanner {
    fun startScanning(timeout: Duration, filters : List<ScanFilter>) : SharedFlow<ScanEvent>
    fun stopScanning()
    val isScanning : SharedFlow<Boolean>
}