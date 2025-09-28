package com.example.augmentedrealityglasses.weather.network

import com.example.augmentedrealityglasses.weather.state.WeatherLocation
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.Date

class APITestService(
    val current: APIWeatherCondition,
    val locations: List<WeatherLocation>,
    val forecasts: APIWeatherForecasts
) : RetrofitService {
    override suspend fun getCurrentWeather(lat: String, lon: String, units: String, appId: String) =
        current

    override suspend fun getLocations(q: String, limit: String, appId: String) = locations

    override suspend fun getWeatherForecasts(
        lat: String, lon: String, units: String, appId: String, cnt: String
    ) = forecasts
}

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherRepositoryImplTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var testService: APITestService
    private lateinit var repo: WeatherRepository

    @Before
    fun setup() {
        val currentWeather = APIWeatherCondition(
            _weather = listOf(APIWeather("Clear", "800", "01d", "clear sky")),
            coord = APICoord("45.0", "9.0"),
            main = APIMain("25.0", "26.0", "22.0", "27.0", "1013", 50),
            wind = APIWind("3.5"),
            sys = APISys(
                country = "IT",
                sunrise = Date(1700000000L * 1000),
                sunset = Date(1700040000L * 1000)
            ),
            name = "Milan",
            dt = Date(1700012345L * 1000)
        )

        val locations = listOf(
            WeatherLocation(
                name = "Milan",
                lat = "45.0",
                lon = "9.0",
                country = "IT",
                state = null
            )
        )

        val forecasts = APIWeatherForecasts(
            list = listOf(
                APIForecast(
                    dt = Date(1700016000L * 1000),
                    main = APIMain("24.0", "24.0", "22.0", "25.0", "1012", 55),
                    _weather = listOf(APIWeather("Clear", "800", "01d", "")),
                    wind = APIWind("3.0")
                )
            )
        )

        testService = APITestService(
            current = currentWeather,
            locations = locations,
            forecasts = forecasts
        )

        repo = WeatherRepositoryImpl(testService, dispatcher)
    }

    @Test
    fun getCurrentWeatherSuccess() = runTest(dispatcher) {
        val expected = testService.current
        val res = repo.getCurrentWeather("45.0", "9.0")

        require(res is APIResult.Success)

        assertThat(res.value).isEqualTo(expected)
    }

    @Test
    fun searchLocationsSuccess() = runTest(dispatcher) {

        val expected = testService.locations
        val res = repo.searchLocations("Milan")

        require(res is APIResult.Success)
        assertThat(res.value).isEqualTo(expected)
    }

    @Test
    fun getWeatherForecastsSuccess() = runTest(dispatcher) {
        val expected = testService.forecasts
        val res = repo.getWeatherForecasts("45.0", "9.0")

        require(res is APIResult.Success)
        assertThat(res.value).isEqualTo(expected)
    }
}