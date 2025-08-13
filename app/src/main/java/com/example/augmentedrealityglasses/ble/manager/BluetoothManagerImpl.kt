package com.example.augmentedrealityglasses.ble.manager

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED
import android.content.Context
import android.content.IntentFilter
import com.example.augmentedrealityglasses.ble.manager.monitor.SessionMonitor
import com.example.augmentedrealityglasses.ble.manager.monitor.SessionMonitorImpl
import com.example.augmentedrealityglasses.ble.peripheral.Peripheral
import com.example.augmentedrealityglasses.ble.peripheral.PeripheralImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class BluetoothManagerImpl(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : BluetoothManager {

    companion object {
        private val TAG = BluetoothManagerImpl::class.simpleName
    }

    override val bluetoothOn: StateFlow<BluetoothState>

    override var closed: Boolean = false
        private set

    private val bluetoothStateReceiver: BluetoothStateReceiver

    private val monitors: MutableList<SessionMonitor> = ArrayList()

    init {
        val bluetoothManager: android.bluetooth.BluetoothManager =
            context.getSystemService(android.bluetooth.BluetoothManager::class.java)
        val adapter: BluetoothAdapter? = bluetoothManager.adapter
        require(adapter != null) {
            "The device must support bluetooth"
        }

        // monitor bluetooth state
        bluetoothStateReceiver = BluetoothStateReceiver()
        bluetoothOn = bluetoothStateReceiver.bluetoothState
            .stateIn(scope, SharingStarted.Eagerly, adapter.state.toBluetoothState())
        val filter = IntentFilter(ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(bluetoothStateReceiver, filter)
    }

    override fun createStub(device: BluetoothDevice) : Peripheral {
        val idx = monitors.indexOfFirst { it.peripheral.mac == device.address }

        /* an existing session for the peripheral already exists, close the session and recreate a new one */
        if (idx != -1) {
            val currentMonitor = monitors[idx]
            currentMonitor.close()
            monitors.removeAt(idx)
        }
        // create the stub, start managing it and return it
        val peripheral : Peripheral = PeripheralImpl(device,context)
        monitors.add(SessionMonitorImpl(peripheral))
        return peripheral
    }

    override fun getStub(mac: String): Peripheral? {
        val peripheral = monitors.find { it.peripheral.mac == mac }?.peripheral
        return peripheral
    }

    override fun close() {
        scope.cancel()
        context.unregisterReceiver(bluetoothStateReceiver)
        closed = true
    }
}