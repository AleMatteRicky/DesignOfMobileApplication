package com.example.augmentedrealityglasses.ble.scanner

import android.bluetooth.le.ScanResult

sealed interface ScanEvent

data class ScanSuccess(val scanResult: ScanResult) : ScanEvent

data class ScanError(val state: Int) : ScanEvent