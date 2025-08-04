package com.example.augmentedrealityglasses.ble.viewmodels

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.augmentedrealityglasses.App
import com.example.augmentedrealityglasses.ble.devicedata.RemoteDeviceManager

class FindDeviceViewModel(
    private val bleManager: RemoteDeviceManager
) : ViewModel() {

    fun connect(device: BluetoothDevice) {
        bleManager.setDeviceToManage(device)
        bleManager.connect()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val bleManager = (this[APPLICATION_KEY] as App).container.proxy
                FindDeviceViewModel(
                    bleManager = bleManager
                )
            }
        }
    }

}