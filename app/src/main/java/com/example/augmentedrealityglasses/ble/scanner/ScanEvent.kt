package com.example.augmentedrealityglasses.ble.scanner

import android.bluetooth.le.ScanResult

sealed interface ScanEvent {
    val scanResult: ScanResult?
    val paired : Boolean
    val state : Int
}

data class ScanSuccess(override val scanResult: ScanResult?, override val paired: Boolean = false) : ScanEvent {
    override val state: Int = -1
}

data class ScanError(override val state : Int) : ScanEvent {
    override val paired: Boolean = false
    override val scanResult : ScanResult? = null
}