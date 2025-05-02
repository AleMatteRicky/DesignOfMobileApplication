package com.example.augmentedrealityglasses.ble.device

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

/*
    Proxy between the application and the device.
    It exposes the information from the BleDevice to change the UI
*/
class BleManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) {
    private var _bleDevice: BleDevice? = null
    private var connectionStatus: Flow<DeviceConnectionState>? = null
    private val TAG = "BleManager"

    fun setDataSource(bleDevice: BleDevice) {
        if (_bleDevice != null) {
            Log.d(TAG, "cleaning previous connection")
            // a new device has been added, hence close the flow from the previous one
            close()
        }
        this._bleDevice = bleDevice
    }

    fun connect() {
        Log.d(TAG, "BLEManager received function call to connect")
        connectionStatus = _bleDevice?.connect(context)?.shareIn(
            scope = scope,
            replay = 1,
            started = SharingStarted.Eagerly // prepare the flow for the connection as soon as possible
        )
    }

    fun receiveUpdates(): Flow<DeviceConnectionState> {
        Log.d(TAG, "BLEManager received function call to receiveUpdates")
        require(connectionStatus != null)
        return connectionStatus as Flow<DeviceConnectionState>
    }

    fun send(msg: String) {
        _bleDevice?.send(msg)
    }

    fun close() {
        // reset the state
        scope.coroutineContext.cancelChildren()
        _bleDevice?.close()
    }
}