package com.example.augmentedrealityglasses.ble.scanner

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/**
 * API to scan for devices
 */
interface Scanner {
    /**
     * Scans for devices
     * @param timeout the maximum amount of time to scan for devices
     * @param filters the filters to apply
     * @param settings to apply when scanning
     * @return flow of ScanEvent to inform about the scanning process
     */
    fun scan(
        timeout: Duration,
        filters: List<ScanFilter>?,
        settings: ScanSettings
    ): Flow<ScanEvent>
}