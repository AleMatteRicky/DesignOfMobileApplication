package com.example.augmentedrealityglasses.weather.network

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

suspend fun <T> safeApiCall(
    dispatcher: CoroutineDispatcher,
    apiCall: suspend () -> T
): APIResult<T> {
    return withContext(dispatcher) {
        try {
            APIResult.Success(
                apiCall()
            )
        } catch (throwable: Throwable) {
            when (throwable) {
                is IOException -> APIResult.NetworkError
                is HttpException -> {
                    val code = throwable.code()
                    val message = throwable.response()?.errorBody()?.string()
                    APIResult.GenericError(code, message.orEmpty())
                }

                else -> {
                    APIResult.GenericError(null, "")
                }
            }
        }
    }
}
