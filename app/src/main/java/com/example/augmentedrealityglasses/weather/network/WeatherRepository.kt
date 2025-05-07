package com.example.augmentedrealityglasses.weather.network

import com.example.augmentedrealityglasses.weather.state.WeatherLocation

interface WeatherRepository {
    suspend fun getCurrentWeather(lat: String, lon: String): APIResult<APIWeatherCondition>
    suspend fun searchLocations(query: String): APIResult<List<WeatherLocation>>
    suspend fun getWeatherForecasts(lat: String, lon: String): APIResult<APIWeatherForecasts>
}