package com.example.augmentedrealityglasses.ble.manager

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class BluetoothStateReceiver : BroadcastReceiver() {
    private val _bluetoothState = MutableSharedFlow<BluetoothState>(replay = 1)
    val bluetoothState : SharedFlow<BluetoothState> = _bluetoothState

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            _bluetoothState.tryEmit(state.toBluetoothState())
        }
    }
}