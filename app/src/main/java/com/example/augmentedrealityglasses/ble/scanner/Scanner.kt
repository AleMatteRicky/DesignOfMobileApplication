package com.example.augmentedrealityglasses.ble.scanner

import android.bluetooth.le.ScanFilter
import com.example.augmentedrealityglasses.ble.peripheral.Peripheral
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlin.time.Duration

interface Scanner {
    fun startScanning(timeout: Duration, filters : List<ScanFilter>) : Flow<Peripheral>
    fun stopScanning()
    val isScanning : SharedFlow<Boolean>
}