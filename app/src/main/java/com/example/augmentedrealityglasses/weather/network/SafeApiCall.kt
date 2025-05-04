package com.example.augmentedrealityglasses.weather.network

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

suspend fun <T> safeApiCall(
    dispatcher: CoroutineDispatcher,
    apiCall: suspend () -> T
): ResultWrapper<T> {
    return withContext(dispatcher) {
        try {
            ResultWrapper.Success(
                apiCall()
            )
        } catch (throwable: Throwable) {
            when (throwable) {
                is IOException -> ResultWrapper.NetworkError
                is HttpException -> {
                    val code = throwable.code()
                    val message = throwable.response()?.errorBody()?.string()
                    ResultWrapper.GenericError(code, message.orEmpty())
                }

                else -> {
                    //TODO: log
                    ResultWrapper.GenericError(null, "")
                }
            }
        }
    }
}
