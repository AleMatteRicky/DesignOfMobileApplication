package com.example.augmentedrealityglasses.ble.manager

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED
import android.content.Context
import android.content.IntentFilter
import com.example.augmentedrealityglasses.ble.manager.monitor.Monitor
import com.example.augmentedrealityglasses.ble.manager.monitor.MonitorImpl
import com.example.augmentedrealityglasses.ble.peripheral.Peripheral
import com.example.augmentedrealityglasses.ble.scanner.Scanner
import com.example.augmentedrealityglasses.ble.scanner.ScannerImpl
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
    private val TAG = BluetoothManagerImpl::class.qualifiedName

    override val bluetoothOn: StateFlow<BluetoothState>

    override val scanner: Scanner

    override var closed: Boolean = false
        private set

    private val peripherals: MutableList<Peripheral> = ArrayList()

    private val bluetoothStateReceiver: BluetoothStateReceiver

    private val monitors : MutableList<Monitor> = ArrayList()

    init {
        // setup scanner
        val bluetoothManager: android.bluetooth.BluetoothManager =
            context.getSystemService(android.bluetooth.BluetoothManager::class.java)
        val adapter: BluetoothAdapter? = bluetoothManager.adapter
        require(adapter != null) {
            "The device must support bluetooth"
        }
        scanner = ScannerImpl(adapter, context)

        // monitor bluetooth state
        bluetoothStateReceiver = BluetoothStateReceiver()
        bluetoothOn = bluetoothStateReceiver.bluetoothState
            .stateIn(scope, SharingStarted.Eagerly, adapter.state.toBluetoothState())
        val filter = IntentFilter(ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(bluetoothStateReceiver, filter)
    }

    override fun manage(peripheral: Peripheral) {
        // if not monitored yet, start monitoring it
        if (!peripherals.contains(peripheral)) {
            peripherals.add(peripheral)
            monitor(peripheral)
        }
    }

    override fun getPeripheral(mac: String): Peripheral? {
        return peripherals.find { it.mac == mac }
    }

    override fun close() {
        scope.cancel()
        context.unregisterReceiver(bluetoothStateReceiver)
        closed = true
    }

    fun monitor(peripheral: Peripheral) {
        monitors.add(MonitorImpl(peripheral))
    }
}