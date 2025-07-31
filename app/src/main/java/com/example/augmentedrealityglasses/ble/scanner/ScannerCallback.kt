package com.example.augmentedrealityglasses.ble.scanner

import android.bluetooth.le.ScanCallback
import kotlinx.coroutines.flow.SharedFlow

abstract class ScannerCallback : ScanCallback() {
    abstract val scanEvent : SharedFlow<ScanEvent>
}