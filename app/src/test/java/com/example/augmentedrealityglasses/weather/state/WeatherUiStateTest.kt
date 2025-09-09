package com.example.augmentedrealityglasses.weather.state

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherUiStateTest {

    @Test
    fun `getFullName and toString return empty when name is empty`() {
        val loc = WeatherLocation(
            name = "",
            lat = "0",
            lon = "0",
            country = "IT",
            state = null
        )

        assertEquals("", loc.getFullName())
        assertEquals("", loc.toString())
    }

    @Test
    fun `getFullName without state returns Name, Country and toString returns Name (Country)`() {
        val loc = WeatherLocation(
            name = "Rome",
            lat = "41.9",
            lon = "12.5",
            country = "IT",
            state = null
        )

        assertEquals("Rome, IT", loc.getFullName())
        assertEquals("Rome (IT)", loc.toString())
    }

    @Test
    fun `getFullName with state appends state in parentheses and toString is Name (Country)`() {
        val loc = WeatherLocation(
            name = "Rome",
            lat = "41.9",
            lon = "12.5",
            country = "IT",
            state = "Lazio"
        )

        assertEquals("Rome, IT (Lazio)", loc.getFullName())
        assertEquals("Rome (IT)", loc.toString())
    }

    @Test
    fun `getFullName with empty state`() {
        val loc = WeatherLocation(
            name = "Rome",
            lat = "41.9",
            lon = "12.5",
            country = "IT",
            state = ""
        )

        assertEquals("Rome, IT", loc.getFullName())
        assertEquals("Rome (IT)", loc.toString())
    }
}