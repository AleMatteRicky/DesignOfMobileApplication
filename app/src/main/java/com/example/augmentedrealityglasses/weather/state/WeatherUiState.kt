package com.example.augmentedrealityglasses.weather.state

import com.example.augmentedrealityglasses.R
import com.example.augmentedrealityglasses.weather.constants.Constants
import java.util.Date

data class WeatherUiState(
    val conditions: List<WeatherCondition>
)

data class WeatherCondition(
    val main: String,
    val description: String,
    private val conditionId: Int,
    private val apiIconName: String,
    val temp: Int,
    val feelsLike: Int,
    val tempMin: Int,
    val tempMax: Int,
    val pressure: Int,
    val dateTime: Date,
    val isCurrent: Boolean
) {
    val iconId: Int
        get() = getWeatherIconId(conditionId, apiIconName)

    //TODO (for ble)
    override fun toString(): String {
        return this.dateTime.toString() + " - " + "T: " + this.temp + Constants.TEMPERATURE_UNIT + "; P: " + this.pressure + Constants.PRESSURE_UNIT
    }
}

/**
 * This function provides the resource id of the weather icons
 */
private fun getWeatherIconId(conditionId: Int, iconId: String): Int {
    //TODO: add author's credits of pngs

    var id = R.drawable.clear //TODO: handle exceptions

    //TODO: use constants
    if (conditionId in 200..232) {
        // Thunderstorm
        id = R.drawable.thunderstorm
    } else if (conditionId in 300..531) {
        // Drizzle or Rain
        id = R.drawable.rain
    } else if (conditionId in 600..622) {
        //Snow
        id = R.drawable.snow
    } else if (conditionId == 800 && iconId == "01d") {
        //Clear (day)
        id = R.drawable.clear
    } else if (conditionId == 800 && iconId == "01n") {
        //Clear (night)
        id = R.drawable.clear_night
    } else if (conditionId in 801..804) {
        //Clouds
        id = R.drawable.clouds
    }

    return id
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