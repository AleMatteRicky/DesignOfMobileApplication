package com.example.augmentedrealityglasses.ble.manager.monitor

import android.util.Log
import com.example.augmentedrealityglasses.ble.peripheral.Peripheral
import com.example.augmentedrealityglasses.ble.peripheral.bonding.BondState
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class MonitorImpl(
    override val peripheral: Peripheral
) : Monitor {
    private val TAG = Monitor::class.qualifiedName

    var toClose: AtomicBoolean = AtomicBoolean(false)

    val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    override fun close() {
        scope.cancel()
    }

    private suspend fun disconnectPeripheral() {
        peripheral.disconnect()
        scope.launch {
            delay(2000)
            if (toClose.get()) {
                peripheral.close()
            }
        }
    }

    private fun startMonitoringBondingState() {
        scope.launch {
            peripheral.bondingState.collect {
                when (it) {
                    BondState.Bonded -> {
                        Log.d(TAG, "Bonding completed successfully")
                        peripheral.discoverServices()
                    }

                    BondState.Bonding -> {
                        Log.d(TAG, "Bonding is in progress, do nothing")
                    }

                    BondState.None -> {
                        Log.d(TAG, "Lost a previous bound or a current one just failed")
                        disconnectPeripheral()
                    }

                    BondState.Unknown -> {
                        Log.d(TAG, "No bonding info yet")
                    }
                }
            }
        }
    }

    private fun startMonitoringTheConnectionState() {
        scope.launch {
            peripheral.connectionState.collect {
                if (it is ConnectionState.Connected) {
                    var discover = true
                    if (it.bondState == BondState.Bonded) {
                        Log.d(TAG, "Bonding established successfully")
                    } else if (it.bondState == BondState.None) {
                        Log.d(TAG, "No bonding")
                    } else if (it.bondState == BondState.Bonding) {
                        Log.d(TAG, "Bonding, wait")
                        discover = false
                    }

                    if (discover) {
                        peripheral.discoverServices()
                    }
                } else if (it is ConnectionState.Disconnected) {
                    if (it.userInitiatedDisconnection) {
                        Log.d(TAG, "User initiated disconnection")
                    } else {
                        Log.d(TAG, "Error caused disconnection. Reason: ${it.reason}")
                    }
                    peripheral.close()
                    toClose.set(false)
                } else {
                    Log.d(TAG, "Intermediate operation")
                }
            }
        }
    }

    init {
        startMonitoringBondingState()
        startMonitoringTheConnectionState()
    }
}