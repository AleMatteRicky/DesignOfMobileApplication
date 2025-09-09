package com.example.augmentedrealityglasses.cache

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FakeTimeProvider(private var now: Long) : TimeProvider {
    override fun now(): Long = now
    fun advanceBy(ms: Long) {
        now += ms
    }
}

@Serializable
data class WeatherCondition(val location: String, val temp: Int)

class DataStoreMapCacheTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun newCache(fileName: String = "cache.json"): DataStoreMapCache {
        val file = File(tmp.root, fileName)
        return DataStoreMapCache(file = file)
    }

    @Test
    fun setAndGetPayloadWithIntValue() = runTest {

        val cache = newCache()
        val timeProvider = FakeTimeProvider(1000L)

        cache.set("k1", 50, Int.serializer(), timeProvider)

        val value = cache.getPayload("k1", Int.serializer())

        assertThat(value).isEqualTo(50)
        val entry = cache.getEntry("k1")
        assertThat(entry).isNotNull()
        assertThat(entry!!.savedAt).isEqualTo(1000L)
    }

    @Test
    fun setAndGetPayloadWithWeatherConditionValue() = runTest {
        val cache = newCache()
        val timeProvider = FakeTimeProvider(10L)

        cache.set(
            "user",
            WeatherCondition("Milan", 26),
            WeatherCondition.serializer(),
            timeProvider
        )
        val value = cache.getPayload("user", WeatherCondition.serializer())

        assertThat(value).isEqualTo(WeatherCondition("Milan", 26))
    }

    @Test
    fun getIfValidWithFreshData() = runTest {
        val cache = newCache()
        val time = FakeTimeProvider(0L)

        cache.set("k", "v", String.serializer(), time)

        val policy = MaxAgePolicy(maxAgeMillis = 1000L)

        time.advanceBy(999L)

        val v = cache.getIfValid("k", policy, String.serializer(), time)
        assertThat(v).isEqualTo("v")
    }

    @Test
    fun getIfValidWithInvalidData() = runTest {
        val cache = newCache()
        val time = FakeTimeProvider(0L)

        cache.set("k", "v", String.serializer(), time)

        val policy = MaxAgePolicy(maxAgeMillis = 1000L)
        time.advanceBy(1001L)

        val v = cache.getIfValid("k", policy, String.serializer(), time)
        assertThat(v).isNull()
    }

    @Test
    fun getIfValidWithNeverExpiresPolicy() = runTest {
        val cache = newCache()
        val time = FakeTimeProvider(0L)

        cache.set("k", "v", String.serializer(), time)

        val policy = NeverExpires
        time.advanceBy(100000L)

        val v = cache.getIfValid("k", policy, String.serializer(), time)

        assertThat(v).isEqualTo("v")

        time.advanceBy(100000L)

        assertThat(v).isEqualTo("v")
    }

    @Test
    fun getIfValidWithAlwaysRefreshPolicy() = runTest {
        val cache = newCache()
        val time = FakeTimeProvider(0L)

        cache.set("k", "v", String.serializer(), time)

        val policy = AlwaysRefresh

        time.advanceBy(100000L)

        val v = cache.getIfValid("k", policy, String.serializer(), time)
        assertThat(v).isNull()

        time.advanceBy(100000L)

        assertThat(v).isNull()
    }
}
