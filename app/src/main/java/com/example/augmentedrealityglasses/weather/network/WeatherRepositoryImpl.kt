package com.example.augmentedrealityglasses.weather.network

import com.example.augmentedrealityglasses.weather.state.WeatherLocation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class WeatherRepositoryImpl(
    private val weatherService: RetrofitService = RetrofitProvider.retrofitService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : WeatherRepository {

    override suspend fun getCurrentWeather(
        lat: String,
        lon: String
    ): ResultWrapper<APIWeatherCondition> {
        return safeApiCall(dispatcher) {
            weatherService.getCurrentWeather(lat, lon)
        }
    }

    override suspend fun searchLocations(query: String): ResultWrapper<List<WeatherLocation>> {
        return safeApiCall(dispatcher) {
            weatherService.getLocations(query)
        }
    }

    override suspend fun getWeatherForecasts(
        lat: String,
        lon: String
    ): ResultWrapper<APIWeatherForecasts> {
        return safeApiCall(dispatcher) {
            weatherService.getWeatherForecasts(lat, lon)
        }
    }
}