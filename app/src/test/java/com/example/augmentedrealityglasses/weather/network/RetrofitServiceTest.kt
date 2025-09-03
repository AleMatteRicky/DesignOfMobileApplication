package com.example.augmentedrealityglasses.weather.network

import com.example.augmentedrealityglasses.weather.state.WeatherLocation
import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Date

class RetrofitServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var api: RetrofitService

    @Before
    fun setup() {
        server = MockWebServer()
        val gson = GsonBuilder()
            .registerTypeAdapter(Date::class.java, DateDeserializer())
            .create()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(RetrofitService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getCurrentWeatherDeserializationTest() {
        val body = """
            {
              "weather":[{"id":"800","main":"Clear","description":"clear sky","icon":"01d"}],
              "coord":{"lat":"45.0","lon":"9.0"},
              "main":{"temp":"25.2","feels_like":"26.1","temp_min":"22.0","temp_max":"27.0","pressure":"1013","humidity":55},
              "wind":{"speed":"3.5"},
              "sys":{"country":"IT","sunrise":1700000000,"sunset":1700040000},
              "name":"Milan",
              "dt":1700012345
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))

        val result = runBlocking {
            api.getCurrentWeather("45.0", "9.0", "metric", "key")
        }

        val expected = APIWeatherCondition(
            _weather = listOf(APIWeather("Clear", "800", "01d", "clear sky")),
            coord = APICoord("45.0", "9.0"),
            main = APIMain("25.2", "26.1", "22.0", "27.0", "1013", 55),
            wind = APIWind("3.5"),
            sys = APISys(
                country = "IT",
                sunrise = Date(1700000000L * 1000),
                sunset = Date(1700040000L * 1000)
            ),
            name = "Milan",
            dt = Date(1700012345L * 1000)
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun getLocationsDeserializationTest() {
        val body = """
            [
              {"name":"Milan","lat":"45.4642","lon":"9.1900","country":"IT","state":"Lombardy"},
              {"name":"Milano","lat":"45.4642","lon":"9.1900","country":"IT","state":"Lombardia"}
            ]
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))

        val result = runBlocking {
            api.getLocations(q = "Milan", limit = "5", appId = "key")
        }

        val expected = listOf(
            WeatherLocation(
                name = "Milan",
                lat = "45.4642",
                lon = "9.1900",
                country = "IT",
                state = "Lombardy"
            ),
            WeatherLocation(
                name = "Milano",
                lat = "45.4642",
                lon = "9.1900",
                country = "IT",
                state = "Lombardia"
            )
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun getWeatherForecastsDeserializationTest() {
        val body = """
            {
              "list":[
                {
                  "dt":1700016000,
                  "main":{"temp":"24.0","feels_like":"24.0","temp_min":"22.0","temp_max":"25.0","pressure":"1012","humidity":55},
                  "weather":[{"id":"800","main":"Clear","description":"","icon":"01d"}],
                  "wind":{"speed":"3.0"}
                },
                {
                  "dt":1700026800,
                  "main":{"temp":"23.1","feels_like":"23.5","temp_min":"21.0","temp_max":"24.0","pressure":"1011","humidity":60},
                  "weather":[{"id":"500","main":"Rain","description":"light rain","icon":"10d"}],
                  "wind":{"speed":"4.2"}
                }
              ]
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))

        val result = runBlocking {
            api.getWeatherForecasts(
                lat = "45.0",
                lon = "9.0",
                units = "metric",
                appId = "key",
                cnt = "2"
            )
        }

        val expected = APIWeatherForecasts(
            list = listOf(
                APIForecast(
                    dt = Date(1700016000L * 1000),
                    main = APIMain("24.0", "24.0", "22.0", "25.0", "1012", 55),
                    _weather = listOf(APIWeather("Clear", "800", "01d", "")),
                    wind = APIWind("3.0")
                ),
                APIForecast(
                    dt = Date(1700026800L * 1000),
                    main = APIMain("23.1", "23.5", "21.0", "24.0", "1011", 60),
                    _weather = listOf(APIWeather("Rain", "500", "10d", "light rain")),
                    wind = APIWind("4.2")
                )
            )
        )

        assertThat(result).isEqualTo(expected)
    }
}