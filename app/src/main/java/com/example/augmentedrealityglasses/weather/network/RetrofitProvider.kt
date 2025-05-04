package com.example.augmentedrealityglasses.weather.network

import com.example.augmentedrealityglasses.weather.constants.Constants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

//Singleton object for the retrofit service
object RetrofitProvider {
    val retrofitService: RetrofitService by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.WEATHER_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(RetrofitService::class.java)
    }
}