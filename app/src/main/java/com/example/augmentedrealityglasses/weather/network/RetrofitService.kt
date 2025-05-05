package com.example.augmentedrealityglasses.weather.network

import com.example.augmentedrealityglasses.BuildConfig
import com.example.augmentedrealityglasses.weather.constants.Constants
import com.example.augmentedrealityglasses.weather.state.WeatherCondition
import com.example.augmentedrealityglasses.weather.state.WeatherForecasts
import com.example.augmentedrealityglasses.weather.state.WeatherLocation
import retrofit2.http.GET
import retrofit2.http.Query

interface RetrofitService {

    @GET("data/2.5/weather")
    suspend fun getCurrentWeatherInfo(
        @Query("lat") lat: String,
        @Query("lon") lon: String,
        @Query("units") units: String = Constants.API_UNIT_OF_MEASUREMENT,
        @Query("appid") appId: String = BuildConfig.WEATHER_API_KEY
    ): WeatherCondition

    @GET("geo/1.0/direct")
    suspend fun getLocations(
        @Query("q") q: String,
        @Query("limit") limit: String = Constants.MAX_LOCATIONS_FETCHED,
        @Query("appid") appId: String = BuildConfig.WEATHER_API_KEY
    ): List<WeatherLocation>

    @GET("data/2.5/forecast")
    suspend fun getWeatherForecast(
        @Query("lat") lat: String,
        @Query("lon") lon: String,
        @Query("units") units: String = Constants.API_UNIT_OF_MEASUREMENT,
        @Query("appid") appId: String = BuildConfig.WEATHER_API_KEY,
        @Query("cnt") cnt: String = Constants.NUMBER_OF_TIMESTAMPS_FORECAST
    ): WeatherForecasts
}