package com.example.augmentedrealityglasses.screens

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.augmentedrealityglasses.App
import com.example.augmentedrealityglasses.ble.device.BleDevice
import com.example.augmentedrealityglasses.ble.device.BleManager

class FindDeviceViewModel(
    private val bleManager: BleManager
) : ViewModel() {

    fun connect(device: BluetoothDevice) {
        bleManager.setDataSource(BleDevice(device))
        bleManager.connect()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val bleManager = (this[APPLICATION_KEY] as App).container.bleManager
                FindDeviceViewModel(
                    bleManager = bleManager
                )
            }
        }
    }

}