package com.example.augmentedrealityglasses.weather.network

//Singleton object for the retrofit service
object RetrofitProvider {
    val retroService: RetroService by lazy {
        RetroInstance.getRetroInstance().create(RetroService::class.java)
    }
}