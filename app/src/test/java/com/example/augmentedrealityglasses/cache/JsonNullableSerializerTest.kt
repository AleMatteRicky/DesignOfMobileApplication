package com.example.augmentedrealityglasses.cache

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream

class JsonNullableSerializerReadFromTest {

    @Serializable
    data class WeatherCondition(
        val location: String,
        val temp: Int,
        val current: Boolean = true
    )

    private val serializer = JsonNullableSerializer(WeatherCondition.serializer())

    @Test
    fun readFromValidJsonReturnsObject() = runTest {
        val json = """{"location":"Milan","temp":23,"current":false}"""
        val input = ByteArrayInputStream(json.toByteArray())

        val result = serializer.readFrom(input)

        assertNotNull(result)
        assertEquals(WeatherCondition("Milan", 23, false), result)
    }

    @Test
    fun readFromUnknownKeysAreIgnored() = runTest {
        val json = """
            {
              "location":"Milan",
              "temp":22,
              "current":true,
              "extra":"ignored",
              "nested":{"x":1}
            }
        """.trimIndent()
        val input = ByteArrayInputStream(json.toByteArray())

        val result = serializer.readFrom(input)

        assertEquals(WeatherCondition("Milan", 22, true), result)
    }

    @Test
    fun readFromMissingOptionalFieldUsesDefault() = runTest {
        val json = """{"location":"Milan","temp":23}""" //without "current"
        val input = ByteArrayInputStream(json.toByteArray())

        val result = serializer.readFrom(input)

        assertEquals(WeatherCondition("Milan", 23, true), result)
    }

    @Test
    fun readFromMissingRequiredFieldReturnsNull() = runTest {
        val json = """{"temp":22}"""
        val input = ByteArrayInputStream(json.toByteArray())

        val result = serializer.readFrom(input)

        assertNull(result)
    }

    @Test
    fun readFromMalformedJsonReturnsNull() = runTest {
        val bad = """{ not valid json """
        val input = ByteArrayInputStream(bad.toByteArray())

        val result = serializer.readFrom(input)

        assertNull(result)
    }

    @Test
    fun readFromEmptyStreamReturnsNull() = runTest {
        val input = ByteArrayInputStream(ByteArray(0))

        val result = serializer.readFrom(input)

        assertNull(result)
    }
}