package com.example.augmentedrealityglasses.ble.peripheral

import com.example.augmentedrealityglasses.ble.peripheral.gattevent.GattEvent
import kotlinx.coroutines.flow.SharedFlow
import android.bluetooth.BluetoothGattCallback as AndroidBluetoothGattCallback

abstract class BluetoothGattCallback : AndroidBluetoothGattCallback() {
    /**
     * Generator of GattEvents
     */
    abstract val events: SharedFlow<GattEvent>
}