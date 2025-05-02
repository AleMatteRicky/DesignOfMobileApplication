package com.example.augmentedrealityglasses.weather.retrofit

import com.example.augmentedrealityglasses.BuildConfig
import com.example.augmentedrealityglasses.weather.state.WeatherCondition
import com.example.augmentedrealityglasses.weather.state.WeatherLocation
import retrofit2.http.GET
import retrofit2.http.Query

interface RetroService {

    @GET("data/2.5/weather")
    suspend fun getWeatherInfo(
        @Query("lat") lat: String,
        @Query("lon") lon: String,
        @Query("units") units: String = "metric",
        @Query("appid") appId: String = BuildConfig.WEATHER_API_KEY
    ): WeatherCondition

    @GET("geo/1.0/direct")
    suspend fun getLocations(
        @Query("q") q: String,
        @Query("limit") limit: String = "5",
        @Query("appid") appId: String = BuildConfig.WEATHER_API_KEY
    ): List<WeatherLocation>
}