package com.example.augmentedrealityglasses.weather.state

data class WeatherUiState(
    val condition: WeatherCondition
)

data class WeatherCondition(
    val weather: List<Weather>,
    val main: Main
)

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