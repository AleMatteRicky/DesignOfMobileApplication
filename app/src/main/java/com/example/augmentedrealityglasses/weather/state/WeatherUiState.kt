package com.example.augmentedrealityglasses.weather.state

import com.google.gson.annotations.SerializedName

data class WeatherUiState(
    val condition: WeatherCondition
)

data class WeatherCondition(
    @SerializedName("weather")
    private val _weather: List<Weather>,
    val main: Main
){
    val weather : Weather
        get() = _weather[0]
}

data class Weather(
    val main: String,
    val description: String
)

data class Main(
    val temp: String,
    val pressure: String
)

data class WeatherLocation(
    var name: String,
    var lat: String,
    var lon: String
)