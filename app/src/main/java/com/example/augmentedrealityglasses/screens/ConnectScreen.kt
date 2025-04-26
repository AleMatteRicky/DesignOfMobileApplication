package com.example.augmentedrealityglasses.screens

import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.augmentedrealityglasses.ble.device.BleManager
import kotlinx.coroutines.launch

data class UiDeviceConnectionState(
    val isConnected: Boolean = false,
    val msg: String = "" // TODO: remove, used just for testing notifications
)

// TODO. add SavedStateHandle to retain UI logic after process' death
class ConnectViewModel(
    val bleManager: BleManager
) : ViewModel() {
    private val TAG: String = "ConnectViewModel"

    var uiState by mutableStateOf(UiDeviceConnectionState())
        private set

    fun connect(context: Context) {
        bleManager.connect(context)

        viewModelScope.launch {
            bleManager.receiveUpdates()
                .collect { connectionState ->
                    Log.d(TAG, "new connection state from the ble manager: $connectionState")
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
        fun provideFactory(
            bleManager: BleManager,
            owner: SavedStateRegistryOwner,
            defaultArgs: Bundle? = null,
        ): AbstractSavedStateViewModelFactory =
            object : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle
                ): T {
                    return ConnectViewModel(bleManager) as T
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "connect view model cleared")
    }

}

@Composable
fun ConnectScreen(
    viewModel: ConnectViewModel,
    onClickFeature: (String) -> Unit,
    onClose: () -> Unit
) {
    //val isConnected by remember { mutableStateOf(viewModel.uiState.isConnected) }
    Box(Modifier.fillMaxSize()) {

        if (!viewModel.uiState.isConnected) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }

        // TODO. Make the UI better
        if (viewModel.uiState.isConnected) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        onClickFeature(ScreenName.WEATHER_SCREEN.name)
                    }
                ) {
                    Text(text = ScreenName.WEATHER_SCREEN.name)
                }
                Button(
                    onClick = {
                        onClickFeature(ScreenName.TRANSLATION_SCREEN.name)
                    }
                ) {
                    Text(text = ScreenName.TRANSLATION_SCREEN.name)
                }

                Text(text = "Received: ${viewModel.uiState.msg}", fontWeight = FontWeight.Bold)
            }
        }
    }
}