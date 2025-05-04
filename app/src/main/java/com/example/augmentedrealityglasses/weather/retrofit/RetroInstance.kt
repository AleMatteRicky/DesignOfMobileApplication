package com.example.augmentedrealityglasses.weather.retrofit

import com.example.augmentedrealityglasses.weather.constants.Constants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
//TODO: remove this class
class RetroInstance {

    companion object {

        fun getRetroInstance(): Retrofit {

            return Retrofit.Builder()
                .baseUrl(Constants.WEATHER_API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }
}