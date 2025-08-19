package com.example.augmentedrealityglasses

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.augmentedrealityglasses.ble.devicedata.RemoteDeviceManager
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.ConnectionState
import kotlinx.coroutines.launch

class HomeViewModel(
    private val proxy: RemoteDeviceManager,
) : ViewModel() {

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
    var isExtDeviceConnected by mutableStateOf(false)
        private set


    fun disconnectDevice() {
        proxy.disconnect()
    }
}