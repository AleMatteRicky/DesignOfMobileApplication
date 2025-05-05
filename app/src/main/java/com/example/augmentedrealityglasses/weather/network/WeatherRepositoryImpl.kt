package com.example.augmentedrealityglasses.weather.network

import com.example.augmentedrealityglasses.weather.state.WeatherCondition
import com.example.augmentedrealityglasses.weather.state.WeatherForecasts
import com.example.augmentedrealityglasses.weather.state.WeatherLocation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class WeatherRepositoryImpl(
    private val weatherService: RetrofitService = RetrofitProvider.retrofitService,
    //TODO: check the dispatcher
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : WeatherRepository {

    override suspend fun getCurrentWeatherInfo(
        lat: String,
        lon: String
    ): ResultWrapper<WeatherCondition> {
        return safeApiCall(dispatcher) {
            weatherService.getCurrentWeatherInfo(lat, lon)
        }
    }

    override suspend fun searchLocations(query: String): ResultWrapper<List<WeatherLocation>> {
        return safeApiCall(dispatcher) {
            weatherService.getLocations(query)
        }
    }

    override suspend fun getWeatherForecast(
        lat: String,
        lon: String
    ): ResultWrapper<WeatherForecasts> {
        return safeApiCall(dispatcher) {
            weatherService.getWeatherForecast(lat, lon)
        }
    }
}