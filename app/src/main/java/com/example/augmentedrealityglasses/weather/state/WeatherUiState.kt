package com.example.augmentedrealityglasses.weather.state

import com.example.augmentedrealityglasses.weather.constants.Constants
import java.util.Date


data class WeatherUiState(
    val conditions: List<WeatherCondition>
)

data class WeatherCondition(
    val main: String,
    val description: String,
    val id: Int,
    val icon: String,
    val temp: Int,
    val feelsLike: Int,
    val tempMin: Int,
    val tempMax: Int,
    val pressure: Int,
    val dateTime: Date,
    val isCurrent: Boolean
) {
    //TODO (for ble)
    override fun toString(): String {
        return this.dateTime.toString() + " - " + "T: " + this.temp + Constants.TEMPERATURE_UNIT + "; P: " + this.pressure + Constants.PRESSURE_UNIT
    }
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