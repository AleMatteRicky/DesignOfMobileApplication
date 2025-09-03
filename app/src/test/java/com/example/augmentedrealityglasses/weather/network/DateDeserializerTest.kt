package com.example.augmentedrealityglasses.weather.network

import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import org.junit.Test
import java.util.Date

class DateDeserializerTest {

    private val gson = GsonBuilder()
        .registerTypeAdapter(Date::class.java, DateDeserializer())
        .create()

    data class Sample(val dt: Date)

    @Test
    fun deserializeUnixSecondsTest() {

        val tsInSeconds = 1800000000L

        val json = """{"dt": $tsInSeconds}"""

        val parsed = gson.fromJson(json, Sample::class.java)

        assertThat(parsed.dt.time).isEqualTo(tsInSeconds * 1000)
    }
}