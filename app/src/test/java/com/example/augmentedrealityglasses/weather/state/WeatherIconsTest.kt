package com.example.augmentedrealityglasses.weather.state

import com.example.augmentedrealityglasses.R
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class WeatherIconsTest {

    @Test
    fun `iconId resolves day - night for clear`() {

        val day = WeatherCondition(
            main = "Clear",
            description = "",
            conditionId = 800,
            apiIconName = "01d",
            temp = 25,
            feelsLike = 25,
            tempMin = 20,
            tempMax = 27,
            windSpeed = 2f,
            pressure = 1012,
            humidity = 30,
            sunrise = null,
            sunset = null,
            dateTime = Date(),
            isCurrent = true
        )

        val night = day.copy(apiIconName = "01n")

        assertEquals(R.drawable.clear, day.iconId)
        assertEquals(R.drawable.clear_night, night.iconId)
    }

    @Test
    fun `iconId maps common rain condition`() {
        val rain = WeatherCondition(
            main = "Rain",
            description = "",
            conditionId = 500,
            apiIconName = "10d",
            temp = 14,
            feelsLike = 13,
            tempMin = 12,
            tempMax = 16,
            windSpeed = 3.4f,
            pressure = 1006,
            humidity = 85,
            sunrise = null,
            sunset = null,
            dateTime = Date(),
            isCurrent = false
        )
        assertEquals(R.drawable.rain_1, rain.iconId)
    }

    @Test
    fun `getDailyIconForConditions picks the most frequent conditionId`() {
        val base = WeatherCondition(
            main = "Mixed",
            description = "",
            conditionId = 800,
            apiIconName = "01n",
            temp = 20,
            feelsLike = 19,
            tempMin = 17,
            tempMax = 22,
            windSpeed = 2.2f,
            pressure = 1009,
            humidity = 50,
            sunrise = null,
            sunset = null,
            dateTime = Date(),
            isCurrent = false
        )

        val list = listOf(
            base.copy(conditionId = 801, apiIconName = "02d"),
            base.copy(conditionId = 801, apiIconName = "02n"),
            base.copy(conditionId = 801, apiIconName = "02d"),
            base.copy(conditionId = 800, apiIconName = "01d")
        )

        val icon = getDailyIconForConditions(list)

        assertEquals(R.drawable.clouds_1, icon)
    }

    @Test
    fun `getDailyIconForConditions returns clear for empty list`() {
        val icon = getDailyIconForConditions(emptyList())
        assertEquals(R.drawable.clear, icon)
    }
}