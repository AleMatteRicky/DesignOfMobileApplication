package com.example.augmentedrealityglasses.weather.network

import com.example.augmentedrealityglasses.weather.state.WeatherCondition
import com.example.augmentedrealityglasses.weather.state.WeatherLocation

interface WeatherRepository {
    suspend fun getWeatherInfo(lat: String, lon: String): ResultWrapper<WeatherCondition>
    suspend fun searchLocations(query: String): ResultWrapper<List<WeatherLocation>>
}