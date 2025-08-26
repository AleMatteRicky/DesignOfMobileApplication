package com.example.augmentedrealityglasses.weather.state

import com.example.augmentedrealityglasses.R
import java.util.Date

data class WeatherUiState(
    val conditions: List<WeatherCondition>,
    val selectedDay: Date,
    val location: WeatherLocation,
    val geolocationEnabled: Boolean
)

data class WeatherCondition(
    val main: String,
    val description: String,
    val conditionId: Int,
    val apiIconName: String,
    val temp: Int,
    val feelsLike: Int,
    val tempMin: Int,
    val tempMax: Int,
    val windSpeed: Float, // unit: meter/sec
    val pressure: Int,
    val dateTime: Date,
    val isCurrent: Boolean
) {
    val iconId: Int
        get() = getWeatherIconId(conditionId, apiIconName)
}

/**
 * This function provides the resource id of the weather icons.
 * Visit https://openweathermap.org/weather-conditions for conditionId and iconName
 */
private fun getWeatherIconId(conditionId: Int, iconName: String): Int {
    //Icons made by iconixar from @flaticon

    val isDay = iconName.contains("d")

    return weatherIconMap[conditionId to isDay] ?: R.drawable.clear //TODO: handle exception
}


private typealias Key = Pair<Int, Boolean>

private val weatherIconMap: Map<Key, Int> = buildMap {
    fun putBoth(ids: IntArray, dayRes: Int, nightRes: Int = dayRes) {
        for (id in ids) {
            put(id to true, dayRes)
            put(id to false, nightRes)
        }
    }

    // 2xx – Thunderstorm
    putBoth(
        ids = intArrayOf(200, 230),
        dayRes = R.drawable.thunderstorm_1,
        nightRes = R.drawable.thunderstorm_1_3_night
    )

    putBoth(
        ids = intArrayOf(201, 202, 231, 232, 211, 212, 221),
        dayRes = R.drawable.thunderstorm_2
    )

    putBoth(
        ids = intArrayOf(210),
        dayRes = R.drawable.thunderstorm_3,
        nightRes = R.drawable.thunderstorm_1_3_night
    )

    // 3xx / 5xx – Drizzle / Rain
    putBoth(
        ids = intArrayOf(300, 310, 313, 500, 520),
        dayRes = R.drawable.rain_1,
        nightRes = R.drawable.rain_1_night
    )

    putBoth(
        ids = intArrayOf(301, 311, 321, 501, 521, 531),
        dayRes = R.drawable.rain_2
    )

    putBoth(
        ids = intArrayOf(302, 312, 314, 502, 503, 504, 522),
        dayRes = R.drawable.rain_3
    )

    putBoth(
        ids = intArrayOf(511),
        dayRes = R.drawable.rain_4
    )

    // 6xx – Snow

    putBoth(
        ids = intArrayOf(600, 611, 612, 620),
        dayRes = R.drawable.snow_1,
        nightRes = R.drawable.snow_1_night
    )

    putBoth(
        ids = intArrayOf(615, 616),
        dayRes = R.drawable.snow_2
    )

    putBoth(
        ids = intArrayOf(601, 602, 613, 621, 622),
        dayRes = R.drawable.snow_3
    )

    // 800 – Clear

    putBoth(
        ids = intArrayOf(800),
        dayRes = R.drawable.clear,
        nightRes = R.drawable.clear_night
    )

    // 80x – Clouds

    putBoth(
        ids = intArrayOf(801),
        dayRes = R.drawable.clouds_1,
        nightRes = R.drawable.clouds_1_night
    )

    putBoth(
        ids = intArrayOf(802),
        dayRes = R.drawable.clouds_2
    )

    putBoth(
        ids = intArrayOf(803, 804),
        dayRes = R.drawable.clouds_3
    )
}

data class WeatherLocation(
    val name: String,
    val lat: String,
    val lon: String,
    val country: String,
    val state: String?
) {
    fun getFullName(): String {
        return if (this.name.isEmpty()) {
            ""
        } else if (this.state.isNullOrEmpty()) {
            this.name + ", " + this.country
        } else {
            this.name + ", " + this.country + " (" + this.state + ")"
        }
    }

    override fun toString(): String {
        return if (this.name.isEmpty()) {
            ""
        } else {
            this.name + " (" + this.country + ")"
        }
    }
}

data class DayCondition(
    val date: Date,
    val isCurrent: Boolean,
    val iconId: Int,
    val tempMin: Int,
    val tempMax: Int
)

/**
 * Returns the appropriate icon ID to represent the day's weather based on a list of conditions.
 */
fun getDailyIconForConditions(conditions: List<WeatherCondition>): Int {
    var iconId = R.drawable.clear //TODO: handle

    val conditionId =
        conditions.map { condition -> condition.conditionId }.groupingBy { it }.eachCount()
            .maxByOrNull { it.value }?.key

    if (conditionId != null) {
        iconId = weatherIconMap[conditionId to true] ?: R.drawable.clear //TODO: handle
    }

    return iconId
}