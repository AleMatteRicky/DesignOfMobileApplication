package com.example.augmentedrealityglasses.weather.network

import com.example.augmentedrealityglasses.weather.constants.Constants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetroInstance {

    companion object {

        fun getRetroInstance(): Retrofit {

            return Retrofit.Builder()
                .baseUrl(Constants.BASE_URL_WEATHER)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }
}