package com.example.augmentedrealityglasses.weather.network

import com.example.augmentedrealityglasses.weather.state.WeatherLocation

interface WeatherRepository {
    suspend fun getCurrentWeather(lat: String, lon: String): ResultWrapper<APIWeatherCondition>
    suspend fun searchLocations(query: String): ResultWrapper<List<WeatherLocation>>
    suspend fun getWeatherForecasts(lat: String, lon: String): ResultWrapper<APIWeatherForecasts>
}