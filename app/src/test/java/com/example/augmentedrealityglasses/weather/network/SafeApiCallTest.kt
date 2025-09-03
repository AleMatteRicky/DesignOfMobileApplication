package com.example.augmentedrealityglasses.weather.network

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class SafeApiCallTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher = UnconfinedTestDispatcher()

    data class WeatherSample(val location: String, val temp: Int)

    @Test
    fun apiResultSuccessTest() = runTest(dispatcher) {
        val data = WeatherSample("Milan", 27)
        val res = safeApiCall(dispatcher) { data }
        assertThat((res as APIResult.Success).value).isEqualTo(data)
    }

    @Test
    fun IOExceptionMapsToNetworkError() = runTest(dispatcher) {
        val res = safeApiCall<Int>(dispatcher) { throw IOException("error") }
        assertThat(res).isInstanceOf(APIResult.NetworkError::class.java)
    }

    @Test
    fun HttpExceptionMapsToGenericError() = runTest(dispatcher) {
        val errorBody = """{"msg":"bad request"}""".toResponseBody("application/json".toMediaType())
        val httpEx = HttpException(Response.error<Any>(404, errorBody))

        val res = safeApiCall<Int>(dispatcher) { throw httpEx }

        require(res is APIResult.GenericError)

        assertThat(res.code).isEqualTo(404)
        assertThat(res.error).contains("bad request")
    }

    @Test
    fun unknownExceptionMapsToGenericError() = runTest(dispatcher) {
        val res = safeApiCall<Int>(dispatcher) {
            throw IllegalStateException("unknown error")
        }

        require(res is APIResult.GenericError)

        assertThat(res.code).isNull()
        assertThat(res.error).isEmpty()
    }
}