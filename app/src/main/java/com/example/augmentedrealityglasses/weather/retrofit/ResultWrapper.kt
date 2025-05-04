package com.example.augmentedrealityglasses.weather.retrofit

//TODO: rename package with "network"
sealed class ResultWrapper<out T> {
    data class Success<out T>(val value: T) : ResultWrapper<T>()

    //TODO: make error a data class
    data class GenericError(val code: Int? = null, val error: String) : ResultWrapper<Nothing>()
    object NetworkError : ResultWrapper<Nothing>()
}