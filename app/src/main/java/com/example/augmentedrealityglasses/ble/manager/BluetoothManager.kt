package com.example.augmentedrealityglasses.ble.manager

import com.example.augmentedrealityglasses.ble.peripheral.Peripheral
import com.example.augmentedrealityglasses.ble.scanner.Scanner
import kotlinx.coroutines.flow.StateFlow

/**
 * Bluetooth manager responsible for
 * - managing the state of the bluetooth
 * - scanning and creating peripherals
 */
interface BluetoothManager {
    val bluetoothOn: StateFlow<BluetoothState>
    val scanner : Scanner
    val closed : Boolean

    /**
     * Adds new peripheral to manage.
     */
    fun manage(peripheral: Peripheral)

    /**
     * Gets the peripheral with the provided MAC
     * @param mac of the peripheral
     * @return the peripheral with the provided mac or null if it doesn't exist
     */
    fun getPeripheral(mac: String): Peripheral?

    /**
     * Closes the manager. From this moment on, the manager cannot be utilized anymore
     */
    fun close()
}

