package com.example.augmentedrealityglasses.ble.viewmodels

import android.bluetooth.BluetoothProfile
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.augmentedrealityglasses.App
import com.example.augmentedrealityglasses.ble.device.RemoteDeviceManager
import kotlinx.coroutines.launch

data class UiDeviceConnectionState(
    val isConnected: Boolean = false,
    val msg: String = "" // TODO: remove, used just for testing notifications
)

// TODO. add SavedStateHandle to retain UI logic after process' death
class ConnectViewModel(
    private val bleManager: RemoteDeviceManager
) : ViewModel() {
    private val TAG: String = "ConnectViewModel"

    var uiState by mutableStateOf(UiDeviceConnectionState())
        private set

    init {
        Log.d(TAG, "Creating the ConnectViewModel again")
        viewModelScope.launch {
            bleManager.receiveUpdates()
                .collect { connectionState ->
                    uiState =
                        uiState.copy(
                            isConnected = connectionState.connectionState == BluetoothProfile.STATE_CONNECTED,
                            msg = connectionState.messageReceived
                        )
                }
        }
    }

    // Define ViewModel factory in a companion object
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val bleManager = (this[APPLICATION_KEY] as App).container.bleManager
                ConnectViewModel(
                    bleManager = bleManager
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "connect view model cleared")
    }

    fun closeConnection() {
        bleManager.close()
    }

}