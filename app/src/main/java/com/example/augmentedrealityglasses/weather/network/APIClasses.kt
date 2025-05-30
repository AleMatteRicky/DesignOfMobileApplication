package com.example.augmentedrealityglasses.weather.network

import com.google.gson.annotations.SerializedName
import java.util.Date

data class APIWeatherCondition(
    @SerializedName("weather")
    private val _weather: List<APIWeather>,
    val coord: APICoord,
    val main: APIMain,
    val sys: APISys,
    val name: String,
    val dt: Date
) {
    val weather: APIWeather
        get() = _weather[0]
}

data class APIWeather(
    val main: String,
    val description: String
)

data class APICoord(
    val lat: String,
    val lon: String
)

data class APIMain(
    val temp: String,
    val pressure: String
)

data class APISys(
    val country: String
)

data class APIWeatherForecasts(
    val list: List<APIForecast>
)

data class APIForecast(
    val dt: Date,
    val main: APIMain,
    @SerializedName("weather")
    private val _weather: List<APIWeather>,
) {
    val weather: APIWeather
        get() = _weather[0]
}