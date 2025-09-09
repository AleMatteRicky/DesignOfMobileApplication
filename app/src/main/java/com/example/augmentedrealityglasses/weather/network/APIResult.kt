package com.example.augmentedrealityglasses.weather.network

sealed class APIResult<out T> {
    data class Success<out T>(val value: T) : APIResult<T>()

    //TODO: make error a data class
    data class GenericError(val code: Int? = null, val error: String) : APIResult<Nothing>()
    object NetworkError : APIResult<Nothing>()
}