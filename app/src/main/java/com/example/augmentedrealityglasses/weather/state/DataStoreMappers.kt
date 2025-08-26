package com.example.augmentedrealityglasses.weather.state

import kotlinx.serialization.Serializable
import java.util.Date

// -------- Location --------

@Serializable
class WeatherLocationDS(
    val name: String,
    val lat: String,
    val lon: String,
    val country: String,
    val state: String?
)

fun WeatherLocation.toDS(): WeatherLocationDS =
    WeatherLocationDS(
        name = name,
        lat = lat,
        lon = lon,
        country = country,
        state = state
    )

fun WeatherLocationDS.toModel(): WeatherLocation =
    WeatherLocation(
        name = name,
        lat = lat,
        lon = lon,
        country = country,
        state = state
    )

// -------- Condition --------

@Serializable
class WeatherConditionDS(
    val main: String,
    val conditionId: Int,
    val apiIconName: String,
    val temp: Int,
    val feelsLike: Int,
    val tempMin: Int,
    val tempMax: Int,
    val windSpeed: Float,
    val pressure: Int,
    val dateTimeMillis: Long,
    val isCurrent: Boolean
)

fun WeatherCondition.toDS(): WeatherConditionDS =
    WeatherConditionDS(
        main = main,
        conditionId = conditionId,
        apiIconName = apiIconName,
        temp = temp,
        feelsLike = feelsLike,
        tempMin = tempMin,
        tempMax = tempMax,
        windSpeed = windSpeed,
        pressure = pressure,
        dateTimeMillis = dateTime.time,
        isCurrent = isCurrent
    )

fun WeatherConditionDS.toModel(): WeatherCondition =
    WeatherCondition(
        main = main,
        description = "",
        conditionId = conditionId,
        apiIconName = apiIconName,
        temp = temp,
        feelsLike = feelsLike,
        tempMin = tempMin,
        tempMax = tempMax,
        windSpeed = windSpeed,
        pressure = pressure,
        dateTime = Date(dateTimeMillis),
        isCurrent = isCurrent
    )

@Serializable
class WeatherSnapshot(
    val location: WeatherLocationDS,
    val conditions: List<WeatherConditionDS>
)

fun List<WeatherCondition>.toDSList(): List<WeatherConditionDS> = map { it.toDS() }
fun List<WeatherConditionDS>.toModelList(): List<WeatherCondition> = map { it.toModel() }

// -------- Snapshot helpers --------

fun WeatherSnapshot.toLocationModel(): WeatherLocation = location.toModel()
fun WeatherSnapshot.toConditionsModel(): List<WeatherCondition> = conditions.toModelList()

fun createWeatherSnapshot(
    location: WeatherLocation,
    conditions: List<WeatherCondition>
): WeatherSnapshot = WeatherSnapshot(
    location = location.toDS(),
    conditions = conditions.toDSList()
)