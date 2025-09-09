package com.example.augmentedrealityglasses.cache

import org.junit.Assert.assertTrue
import org.junit.Test

class TimeProviderTest {

    @Test
    fun nowIsWithinSystemTimeWindow() {
        val before = System.currentTimeMillis()
        val now = DefaultTimeProvider.now()
        val after = System.currentTimeMillis()

        assertTrue(now in before..after)
    }

    @Test
    fun nowMovesForwardOverTime() {
        val t1 = DefaultTimeProvider.now()
        Thread.sleep(20)
        val t2 = DefaultTimeProvider.now()

        assertTrue(t2 >= t1)
    }
}