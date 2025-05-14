package com.example.augmentedrealityglasses.weather.network

import com.example.augmentedrealityglasses.weather.constants.Constants
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Date

//Singleton object for the retrofit service
object RetrofitProvider {
    val retrofitService: RetrofitService by lazy {
        val gson = GsonBuilder()
            .registerTypeAdapter(Date::class.java, DateDeserializer())
            .create()

        Retrofit.Builder()
            .baseUrl(Constants.WEATHER_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build().create(RetrofitService::class.java)
    }
}