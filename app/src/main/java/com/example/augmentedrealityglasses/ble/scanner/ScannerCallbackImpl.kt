package com.example.augmentedrealityglasses.ble.scanner

import android.bluetooth.le.ScanResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ScannerCallbackImpl() : ScanCallback() {
    private val _scanEvent : MutableSharedFlow<ScanEvent> = MutableSharedFlow(5)
    override val scanEvent : SharedFlow<ScanEvent> = _scanEvent.asSharedFlow()

    override fun onScanResult(callbackType: Int, result: ScanResult) {
        super.onScanResult(callbackType, result)
        _scanEvent.tryEmit(ScanSuccess(result))
    }

    override fun onScanFailed(errorCode: Int) {
        super.onScanFailed(errorCode)
        _scanEvent.tryEmit(ScanError(errorCode))
    }
}