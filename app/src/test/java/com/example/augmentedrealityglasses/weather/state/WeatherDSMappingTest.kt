package com.example.augmentedrealityglasses.weather.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class WeatherMappingTest {

    @Test
    fun `WeatherLocation toDS - toModel round trip`() {
        val model = WeatherLocation(
            name = "Rome",
            lat = "41.9028",
            lon = "12.4964",
            country = "IT",
            state = "Lazio"
        )

        val ds = model.toDS()
        val back = ds.toModel()

        assertEquals("Rome", ds.name)
        assertEquals("41.9028", ds.lat)
        assertEquals("12.4964", ds.lon)
        assertEquals("IT", ds.country)
        assertEquals("Lazio", ds.state)

        assertEquals(model, back)
    }

    @Test
    fun `WeatherCondition toDS - toModel round trip`() {
        val dt = Date(1700000000000)
        val sunrise = Date(1700000100000)
        val sunset = Date(1700000200000)

        val model = WeatherCondition(
            main = "Clear",
            description = "clear sky",
            conditionId = 800,
            apiIconName = "01d",
            temp = 27,
            feelsLike = 28,
            tempMin = 22,
            tempMax = 30,
            windSpeed = 3.5f,
            pressure = 1012,
            humidity = 40,
            sunrise = sunrise,
            sunset = sunset,
            dateTime = dt,
            isCurrent = true
        )

        val ds = model.toDS()
        val back = ds.toModel()

        assertEquals("Clear", ds.main)
        assertEquals(800, ds.conditionId)
        assertEquals("01d", ds.apiIconName)
        assertEquals(27, ds.temp)
        assertEquals(28, ds.feelsLike)
        assertEquals(22, ds.tempMin)
        assertEquals(30, ds.tempMax)
        assertEquals(3.5f, ds.windSpeed)
        assertEquals(1012, ds.pressure)
        assertEquals(40, ds.humidity)
        assertEquals(sunrise.time, ds.sunriseMillis)
        assertEquals(sunset.time, ds.sunsetMillis)
        assertEquals(dt.time, ds.dateTimeMillis)
        assertTrue(ds.isCurrent)

        assertEquals("Clear", back.main)
        assertEquals("", back.description) //no description in DS
        assertEquals(800, back.conditionId)
        assertEquals("01d", back.apiIconName)
        assertEquals(27, back.temp)
        assertEquals(28, back.feelsLike)
        assertEquals(22, back.tempMin)
        assertEquals(30, back.tempMax)
        assertEquals(3.5f, back.windSpeed)
        assertEquals(1012, back.pressure)
        assertEquals(40, back.humidity)
        assertEquals(sunrise, back.sunrise)
        assertEquals(sunset, back.sunset)
        assertEquals(dt, back.dateTime)
        assertTrue(back.isCurrent)
    }

    @Test
    fun `WeatherCondition toDS - toModel handles null sunrise sunset`() {
        val dt = Date(1700000000000)

        val model = WeatherCondition(
            main = "Clouds",
            description = "scattered clouds",
            conditionId = 801,
            apiIconName = "02n",
            temp = 20,
            feelsLike = 20,
            tempMin = 18,
            tempMax = 22,
            windSpeed = 1.2f,
            pressure = 1008,
            humidity = 60,
            sunrise = null,
            sunset = null,
            dateTime = dt,
            isCurrent = false
        )

        val ds = model.toDS()
        val back = ds.toModel()

        assertNull(ds.sunriseMillis)
        assertNull(ds.sunsetMillis)
        assertNull(back.sunrise)
        assertNull(back.sunset)
        assertEquals(dt, back.dateTime)
    }

    @Test
    fun `WeatherSnapshot helpers convert location and conditions`() {
        val loc = WeatherLocation("Milan", "45.46", "9.19", "IT", "Lombardy")

        val c1 = WeatherCondition(
            main = "Rain",
            description = "",
            conditionId = 500,
            apiIconName = "10d",
            temp = 15,
            feelsLike = 14,
            tempMin = 12,
            tempMax = 16,
            windSpeed = 2.1f,
            pressure = 1005,
            humidity = 80,
            sunrise = null,
            sunset = null,
            dateTime = Date(1700000300000),
            isCurrent = true
        )

        val c2 = c1.copy(
            conditionId = 501,
            apiIconName = "10n",
            isCurrent = false
        )

        val snap = createWeatherSnapshot(loc, listOf(c1, c2))

        val locBack = snap.toLocationModel()
        val listBack = snap.toConditionsModel()

        assertEquals(loc, locBack)
        assertEquals(2, listBack.size)
        assertEquals(500, listBack[0].conditionId)
        assertEquals(501, listBack[1].conditionId)
    }

    @Test
    fun `List mapping helpers work`() {
        val dt = Date(1700000000000)

        val c = WeatherCondition(
            main = "Clear",
            description = "",
            conditionId = 800,
            apiIconName = "01d",
            temp = 25,
            feelsLike = 25,
            tempMin = 20,
            tempMax = 26,
            windSpeed = 3f,
            pressure = 1010,
            humidity = 35,
            sunrise = null,
            sunset = null,
            dateTime = dt,
            isCurrent = true
        )
        val dsList = listOf(c).toDSList()
        val backList = dsList.toModelList()

        assertEquals(1, dsList.size)
        assertEquals(1, backList.size)
        assertEquals(800, backList[0].conditionId)
        assertEquals(dt, backList[0].dateTime)
    }
}