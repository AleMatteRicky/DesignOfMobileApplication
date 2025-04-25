package com.example.augmentedrealityglasses.screens

import android.bluetooth.BluetoothGatt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.augmentedrealityglasses.ble.device.BleManager
import kotlinx.coroutines.launch

data class UiMessageForTranslationState(
    val isSending: Boolean = false,
    val msg: String = "",
    val isConnected: Boolean = true
)

class TranslationViewModel(
    private val bleManager: BleManager
) : ViewModel() {
    var uiState by mutableStateOf(UiMessageForTranslationState())
        private set

    init {
        viewModelScope.launch {
            bleManager.receiveUpdates()
                .collect { connectionState ->
                    uiState = uiState.copy(
                        isConnected = connectionState.connectionState == BluetoothGatt.STATE_CONNECTED,
                        isSending = !connectionState.messageSent && uiState.msg.isNotEmpty()
                    )
                }
        }
    }

    fun send(msg: String) {
        bleManager.send(msg)
        uiState = uiState.copy(msg = msg, isSending = true)
    }

}