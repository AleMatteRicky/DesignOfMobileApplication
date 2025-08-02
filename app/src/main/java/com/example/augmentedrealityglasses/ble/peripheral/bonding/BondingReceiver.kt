package com.example.augmentedrealityglasses.ble.peripheral.bonding

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class BondingReceiver(
    val deviceAddress: String
) : BroadcastReceiver() {
    private val _bondingEvent = MutableSharedFlow<BondEvent>(1)
    val bondingEvent: SharedFlow<BondEvent> = _bondingEvent

    private val TAG = BondingReceiver::class.qualifiedName

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action

        if (ACTION_BOND_STATE_CHANGED == action) {
            Log.d(TAG, "bonding state changed")

            // get the device to which this event refers to
            val device = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            } else {
                intent.getParcelableExtra(
                    BluetoothDevice.EXTRA_DEVICE,
                    BluetoothDevice::class.java
                )
            }

            val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)

            // Ignore updates for other devices
            if (device == null || device.address == null || device.address.equals(deviceAddress))
                return

            _bondingEvent.tryEmit(
                BondEvent(bondState.toBondState())
            )
        }
    }
}