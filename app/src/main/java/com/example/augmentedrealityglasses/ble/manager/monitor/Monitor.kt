package com.example.augmentedrealityglasses.ble.manager.monitor

import com.example.augmentedrealityglasses.ble.peripheral.Peripheral

/**
 * Monitors the peripheral's state
 */
interface Monitor {
    val peripheral : Peripheral

    fun close()
}