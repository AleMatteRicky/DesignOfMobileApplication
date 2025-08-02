package com.example.augmentedrealityglasses.ble.scanner

import android.bluetooth.le.ScanCallback as AndroidScanCallback // rename to not have conflicts
import kotlinx.coroutines.flow.SharedFlow

abstract class ScanCallback : AndroidScanCallback() {
    abstract val scanEvent : SharedFlow<ScanEvent>
}