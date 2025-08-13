package com.example.augmentedrealityglasses.ble.manager

import android.bluetooth.BluetoothDevice
import com.example.augmentedrealityglasses.ble.peripheral.Peripheral
import kotlinx.coroutines.flow.StateFlow

/**
 * Bluetooth manager responsible for
 * - managing the state of the bluetooth
 * - managing peripherals (stubs)
 */
interface BluetoothManager {
    val bluetoothOn: StateFlow<BluetoothState>
    val closed : Boolean

    /**
     * Create a stub for the provided device
     * @param device the device used to create the stub
     * @return a Peripheral representing the stub
     */
    fun createStub(device: BluetoothDevice) : Peripheral

    /**
     * Gets the stub associated with the provided MAC
     * @param mac of the stub
     * @return the stub associated with the provided mac or null if it doesn't exist. If it was the case,
     * use {@link #createStub() BluetoothManager} to create one
     */
    fun getStub(mac: String): Peripheral?

    /**
     * Closes the manager. From this moment on, the manager cannot be utilized anymore: any stub provided
     * by it is closed as well.
     */
    fun close()
}