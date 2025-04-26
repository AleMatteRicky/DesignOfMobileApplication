package com.example.augmentedrealityglasses.weather.state

import com.google.gson.annotations.SerializedName

data class WeatherUiState(
    val condition: WeatherCondition
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
    var name: String,
    var lat: String,
    var lon: String,
    var country: String,
    var state: String?
) {
    fun getFullName(): String {
        return if(this.name.isEmpty()){
            ""
        }else if (this.state.isNullOrEmpty()) {
            this.name + " (" + this.country + ")"
        } else {
            this.name + ", " + this.state.orEmpty() + " (" + this.country + ")"
        }

    }
}