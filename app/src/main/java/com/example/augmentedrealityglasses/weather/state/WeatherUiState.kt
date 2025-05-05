package com.example.augmentedrealityglasses.weather.state

import com.google.gson.annotations.SerializedName
//TODO: improve all classes
data class WeatherUI(
    val main: String,
    val description: String,
    val temp: String,
    val pressure: String,
    val timestamp: String
)

data class WeatherUiState(
    val currentCondition: WeatherCondition,
    val forecasts: WeatherForecasts,
    val shownTimestamp: String
)

data class WeatherCondition(
    @SerializedName("weather")
    private val _weather: List<Weather>,
    val coord: Coord,
    val main: Main,
    val sys: Sys,
    val name: String
) {
    val weather: Weather
        get() = _weather[0]
}

data class Weather(
    val main: String,
    val description: String
)

data class Coord(
    val lat: String,
    val lon: String
)

data class Main(
    val temp: String,
    val pressure: String
)

data class Sys(
    val country: String
)

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
            this.name + " (" + this.country + ")"
        } else {
            this.name + ", " + this.state.orEmpty() + " (" + this.country + ")"
        }

    }
}

data class WeatherForecasts(
    val list: List<Forecast>
)

data class Forecast(
    val dt: String,
    val main: Main,
    @SerializedName("weather")
    private val _weather: List<Weather>,
) {
    val weather: Weather
        get() = _weather[0]
}