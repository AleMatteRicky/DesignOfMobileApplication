package com.example.augmentedrealityglasses.ble

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Mutex to make all gatt operations atomic.
 *
 * This is needed because the BLEStack implemented by Android does not handle multiple commands at the
 * same time, hence immediately returning if a command is fired when another one is still under execution
 */
object GattOperationMutex {
    private val lock = Mutex(locked = false)

    suspend fun <T> withLock(block: suspend () -> T): T {
        return lock.withLock { block() }
    }
}