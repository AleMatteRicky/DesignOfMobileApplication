package com.example.augmentedrealityglasses.weather.state

data class WeatherUiState(
    val condition: WeatherCondition
)

data class Location(
    var lat: String,
    var lon: String,
    var query: String,
    var gpsLocation: Boolean
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

data class LocationAPI(
    val name: String,
    val lat: String,
    val lon: String
)