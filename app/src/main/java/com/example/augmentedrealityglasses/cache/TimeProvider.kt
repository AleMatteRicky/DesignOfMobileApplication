package com.example.augmentedrealityglasses.cache

/**
 * Provides the current time in milliseconds.
 */
interface TimeProvider {
    /**
     * @return The current time in milliseconds since the Unix epoch.
     */
    fun now(): Long
}

/**
 * Default implementation of [TimeProvider] that uses the system clock.
 */
object DefaultTimeProvider : TimeProvider {
    override fun now(): Long {
        return System.currentTimeMillis()
    }
}