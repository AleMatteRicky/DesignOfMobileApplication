package com.example.augmentedrealityglasses.weather.network

import com.example.augmentedrealityglasses.weather.state.WeatherCondition
import com.example.augmentedrealityglasses.weather.state.WeatherLocation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class WeatherRepositoryImpl(
    private val weatherService: RetroService = RetrofitProvider.retroService,
    //TODO: check the dispatcher
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : WeatherRepository {

    override suspend fun getWeatherInfo(lat: String, lon: String): ResultWrapper<WeatherCondition> {
        return safeApiCall(dispatcher) {
            weatherService.getWeatherInfo(lat, lon)
        }
    }

    override suspend fun searchLocations(query: String): ResultWrapper<List<WeatherLocation>> {
        return safeApiCall(dispatcher) {
            weatherService.getLocations(query)
        }
    }
}