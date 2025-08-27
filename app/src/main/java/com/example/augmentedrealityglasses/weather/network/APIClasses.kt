package com.example.augmentedrealityglasses.weather.network

import com.google.gson.annotations.SerializedName
import java.util.Date
import kotlin.math.roundToInt

data class APIWeatherCondition(
    @SerializedName("weather")
    private val _weather: List<APIWeather>,
    val coord: APICoord,
    val main: APIMain,
    val wind: APIWind,
    val sys: APISys,
    val name: String,
    val dt: Date
) {
    val weather: APIWeather
        get() = _weather[0]
}

data class APIWeather(
    val main: String,

    @SerializedName("id")
    val _id: String,

    val icon: String,

    val description: String //FIXME: maybe delete this
) {
    val id: Int
        get() = _id.toInt()
}

data class APICoord(
    val lat: String,
    val lon: String
)

data class APIMain(
    @SerializedName("temp")
    private val _temp: String,

    @SerializedName("feels_like")
    private val _feels_like: String,

    @SerializedName("temp_min")
    private val _temp_min: String,

    @SerializedName("temp_max")
    private val _temp_max: String,

    @SerializedName("pressure")
    private val _pressure: String,

    val humidity: Int
) {
    //TODO: handle "exceptions"
    val temp: Int
        get() = _temp.toDoubleOrNull()?.roundToInt() ?: 0

    val feels_like: Int
        get() = _feels_like.toDoubleOrNull()?.roundToInt() ?: 0

    val temp_min: Int
        get() = _temp_min.toDoubleOrNull()?.roundToInt() ?: 0

    val temp_max: Int
        get() = _temp_max.toDoubleOrNull()?.roundToInt() ?: 0

    val pressure: Int
        get() = _pressure.toDoubleOrNull()?.roundToInt() ?: 0
}

data class APISys(
    val country: String,
    val sunrise: Date,
    val sunset: Date
)

data class APIWind(
    @SerializedName("speed")
    private val _speed: String
) {
    val speed: Float
        get() = ((_speed.toFloatOrNull()
            ?: 0f) * 3.6f * 100).roundToInt() / 100f // Conversion: meters/second → kilometers/hour,then rounded to 2 decimal places
}

data class APIWeatherForecasts(
    val list: List<APIForecast>
)

data class APIForecast(
    val dt: Date,
    val main: APIMain,
    @SerializedName("weather")
    private val _weather: List<APIWeather>,
    val wind: APIWind
) {
    val weather: APIWeather
        get() = _weather[0]
}