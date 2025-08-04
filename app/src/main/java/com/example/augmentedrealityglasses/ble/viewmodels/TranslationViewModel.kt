package com.example.augmentedrealityglasses.ble.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.augmentedrealityglasses.App
import com.example.augmentedrealityglasses.ble.devicedata.RemoteDeviceManager

//FIXME: delete this in future, now just for testing
data class UiMessageForTranslationState(
    val isSending: Boolean = false,
    val msg: String = "",
    val isConnected: Boolean = true
)

class TranslationViewModel(
    private val bleManager: RemoteDeviceManager
) : ViewModel() {
    var uiState by mutableStateOf(UiMessageForTranslationState())
        private set

    private val TAG = "TranslationViewModel"

    init {
        /*
        see: connect view model
        viewModelScope.launch {
            bleManager.receiveUpdates()
                .collect { connectionState ->
                    uiState = uiState.copy(
                        isConnected = connectionState.connectionState == BluetoothGatt.STATE_CONNECTED,
                        isSending = !connectionState.messageSent && uiState.msg.isNotEmpty()
                    )
                }
        }
         */
    }

    fun send(msg: String) {
        //bleManager.send(msg)
        uiState = uiState.copy(msg = msg, isSending = true)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "$TAG cleared")
    }

    // Define ViewModel factory in a companion object
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val bleManager = (this[APPLICATION_KEY] as App).container.proxy
                TranslationViewModel(
                    bleManager = bleManager
                )
            }
        }
    }
}