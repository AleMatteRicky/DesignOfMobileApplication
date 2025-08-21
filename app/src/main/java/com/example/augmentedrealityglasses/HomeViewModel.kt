package com.example.augmentedrealityglasses

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.augmentedrealityglasses.ble.ESP32Proxy
import com.example.augmentedrealityglasses.ble.devicedata.RemoteDeviceManager
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.ConnectionState
import kotlinx.coroutines.launch

class HomeViewModel(
    private val proxy: RemoteDeviceManager,
) : ViewModel() {

    var bondedDevices: List<BluetoothDevice> by mutableStateOf(emptyList())

    //Initialize the viewModel
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val bleManager =
                    (this[APPLICATION_KEY] as App).container.proxy
                HomeViewModel(
                    proxy = bleManager
                )
            }
        }
    }

    // Start listening for Bluetooth packets
    init {
        viewModelScope.launch {
            try {
                proxy.receiveUpdates()
                    .collect { connectionState ->
                        isExtDeviceConnected =
                            connectionState.connectionState is ConnectionState.Connected
                    }
            } catch (_: Exception) {

            }
        }
    }

    // Tracks the Bluetooth connection status with the external device
    var isExtDeviceConnected by mutableStateOf(true)
        private set


    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice): Boolean {
        if (device.address != ESP32Proxy.ESP32MAC) {
            return false
        }

        //FIXME: in case the device is not online?
        proxy.setDeviceToManage(device)
        proxy.connect()
        return true
    }

    fun disconnectDevice() {
        //FIXME: switch isExtDeviceConnected flag to false
        proxy.disconnect()
    }

    fun refreshBondedDevices(context: Context, adapter: BluetoothAdapter?) {
        if (ContextCompat.checkSelfPermission(context, BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            bondedDevices = emptyList()
        } else {
            bondedDevices = adapter?.bondedDevices.orEmpty().toList()
        }
    }
}