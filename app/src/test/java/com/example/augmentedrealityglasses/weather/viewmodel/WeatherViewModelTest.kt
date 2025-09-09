package com.example.augmentedrealityglasses.weather.viewmodel

import android.content.Context
import android.content.res.Resources
import android.util.Log
import com.example.augmentedrealityglasses.ble.devicedata.RemoteDeviceData
import com.example.augmentedrealityglasses.ble.devicedata.RemoteDeviceManager
import com.example.augmentedrealityglasses.ble.peripheral.bonding.BondState
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.ConnectionState
import com.example.augmentedrealityglasses.cache.Cache
import com.example.augmentedrealityglasses.cache.CachePolicy
import com.example.augmentedrealityglasses.update.BluetoothUpdateStatus
import com.example.augmentedrealityglasses.weather.network.APICoord
import com.example.augmentedrealityglasses.weather.network.APIForecast
import com.example.augmentedrealityglasses.weather.network.APIMain
import com.example.augmentedrealityglasses.weather.network.APIResult
import com.example.augmentedrealityglasses.weather.network.APISys
import com.example.augmentedrealityglasses.weather.network.APIWeather
import com.example.augmentedrealityglasses.weather.network.APIWeatherCondition
import com.example.augmentedrealityglasses.weather.network.APIWeatherForecasts
import com.example.augmentedrealityglasses.weather.network.APIWind
import com.example.augmentedrealityglasses.weather.network.WeatherRepository
import com.example.augmentedrealityglasses.weather.state.WeatherLocation
import com.google.android.gms.location.FusedLocationProviderClient
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import java.util.Calendar
import java.util.Date
import kotlin.test.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Mocks
    private lateinit var repository: WeatherRepository
    private lateinit var proxy: RemoteDeviceManager
    private lateinit var cache: Cache
    private lateinit var cachePolicy: CachePolicy

    private lateinit var vm: WeatherViewModel

    private lateinit var defaultUpdates: StateFlow<RemoteDeviceData>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0

        val _updates = MutableStateFlow(RemoteDeviceData(ConnectionState.Disconnected("", false)))
        defaultUpdates = _updates.asStateFlow()

        repository = mockk()
        proxy = mockk {
            coEvery { isConnected() } returns false
            every { receiveUpdates() } returns defaultUpdates
            coEvery { send(any()) } just Runs
        }
        cache = mockk(relaxed = true)
        cachePolicy = mockk(relaxed = true)

        vm = WeatherViewModel(
            repository = repository,
            proxy = proxy,
            cache = cache,
            cachePolicy = cachePolicy
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `searchLocations updates list and flag`() = runTest {
        val results = listOf(
            WeatherLocation("Milano", "45.4642", "9.1900", "IT", "Lombardia"),
            WeatherLocation("Milano Marittima", "44.2773", "12.3502", "IT", "Emilia-Romagna")
        )
        coEvery { repository.searchLocations("milano") } returns APIResult.Success(results)

        vm.searchLocations("milano")
        advanceUntilIdle()

        assert(vm.searchedLocations.size == 2)
        assert(vm.showNoResults)
    }

    @Test
    fun `searchLocations handles network error`() = runTest {
        coEvery { repository.searchLocations("nowhere") } returns APIResult.NetworkError

        vm.searchLocations("nowhere")
        advanceUntilIdle()

        assert(vm.searchedLocations.isEmpty())
        assert(vm.errorMessage.isNotEmpty())
        assert(!vm.showNoResults)
    }

    @Test
    fun `clearSearchedLocationList empties results`() = runTest {
        coEvery { repository.searchLocations("milano") } returns APIResult.Success(
            listOf(WeatherLocation("Milano", "45.46", "9.19", "IT", "Lombardia"))
        )
        vm.searchLocations("milano")
        advanceUntilIdle()
        assert(vm.searchedLocations.isNotEmpty())
        vm.clearSearchedLocationList()

        assert(vm.searchedLocations.isEmpty())
    }

    @Test
    fun `updateQuery updates query field`() = runTest {
        vm.updateQuery("rome")
        assert(vm.query == "rome")
    }

    @Test
    fun `hideNoResult resets flag`() = runTest {
        coEvery { repository.searchLocations("x") } returns APIResult.Success(
            listOf(WeatherLocation("X", "1", "2", "IT", "S"))
        )
        vm.searchLocations("x")
        advanceUntilIdle()
        assert(vm.showNoResults)

        vm.hideNoResult()

        assert(!vm.showNoResults)
    }

    @Test
    fun `hideErrorMessage clears message`() = runTest {
        coEvery { repository.searchLocations("err") } returns APIResult.NetworkError
        vm.searchLocations("err")
        advanceUntilIdle()
        assert(vm.errorMessage.isNotEmpty())

        vm.hideErrorMessage()

        assert(vm.errorMessage.isEmpty())
    }

    @Test
    fun `changeSelectedDay normalizes to start of day`() = runTest {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2024)
            set(Calendar.MONTH, Calendar.MAY)
            set(Calendar.DAY_OF_MONTH, 5)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 45)
            set(Calendar.SECOND, 30)
            set(Calendar.MILLISECOND, 987)
        }
        val noisyDate = cal.time

        vm.changeSelectedDay(noisyDate)
        val set = Calendar.getInstance().apply { time = vm.weatherState.value.selectedDay }

        assert(set.get(Calendar.YEAR) == 2024)
        assert(set.get(Calendar.MONTH) == Calendar.MAY)
        assert(set.get(Calendar.DAY_OF_MONTH) == 5)
        assert(set.get(Calendar.HOUR_OF_DAY) == 0)
        assert(set.get(Calendar.MINUTE) == 0)
        assert(set.get(Calendar.SECOND) == 0)
        assert(set.get(Calendar.MILLISECOND) == 0)
    }

    @Test
    fun `isDataAvailable is false initially`() = runTest {
        assert(!vm.isDataAvailable())
    }

    @Test
    fun `bluetoothUpdateStatus reflects device connection stream`() = runTest {
        val updates = MutableStateFlow(RemoteDeviceData(ConnectionState.Disconnected("", false)))
        val connectedProxy = mockk<RemoteDeviceManager> {
            coEvery { isConnected() } returns true
            every { receiveUpdates() } returns updates.asStateFlow()
            coEvery { send(any()) } just Runs
        }

        val vm2 = WeatherViewModel(
            repository = repository,
            proxy = connectedProxy,
            cache = cache,
            cachePolicy = cachePolicy
        )
        advanceUntilIdle()

        assert(vm2.bluetoothUpdateStatus == BluetoothUpdateStatus.DEVICE_DISCONNECTED)

        updates.value = RemoteDeviceData(ConnectionState.Connected(BondState.Bonded))
        advanceUntilIdle()

        assert(vm2.bluetoothUpdateStatus == BluetoothUpdateStatus.DEVICE_CONNECTED)

        vm2.hideBluetoothUpdate()
        assert(vm2.bluetoothUpdateStatus == BluetoothUpdateStatus.NONE)
    }

    @Test
    fun `refreshWeatherInfos with geolocation OFF uses existing latlon and does not touch fused`() =
        runTest {
            val manualLoc = WeatherLocation("Milano", "45.4642", "9.1900", "IT", "Lombardia")

            val now = Date()
            val apiCurrentManual = apiCurrentWeather(
                name = manualLoc.name,
                lat = manualLoc.lat,
                lon = manualLoc.lon,
                country = manualLoc.country,
                dt = now
            )
            val apiForecastsManual = apiForecasts(
                listOf(apiForecastItem(Date(now.time + 3_600_000), 23, 1011, "02d", 801))
            )

            coEvery {
                repository.getCurrentWeather(
                    manualLoc.lat,
                    manualLoc.lon
                )
            } returns APIResult.Success(apiCurrentManual)
            coEvery {
                repository.getWeatherForecasts(
                    manualLoc.lat,
                    manualLoc.lon
                )
            } returns APIResult.Success(apiForecastsManual)
            vm.getWeatherOfSelectedLocation(manualLoc)
            advanceUntilIdle()
            assert(!vm.weatherState.value.geolocationEnabled)

            val apiCurrentRefreshed = apiCurrentWeather(
                name = "Milano",
                lat = manualLoc.lat,
                lon = manualLoc.lon,
                country = "IT",
                dt = Date(now.time + 10_000)
            )
            val apiForecastsRefreshed = apiForecasts(
                listOf(apiForecastItem(Date(now.time + 4_000_000), 21, 1009, "10d", 500))
            )
            coEvery {
                repository.getCurrentWeather(
                    manualLoc.lat,
                    manualLoc.lon
                )
            } returns APIResult.Success(apiCurrentRefreshed)
            coEvery {
                repository.getWeatherForecasts(
                    manualLoc.lat,
                    manualLoc.lon
                )
            } returns APIResult.Success(apiForecastsRefreshed)

            val fused = mockk<FusedLocationProviderClient>(relaxed = true)

            val ctx = mockk<Context>()
            val res = mockk<Resources>()
            every { ctx.resources } returns res
            every { res.getResourceEntryName(any()) } returns "any"

            vm.refreshWeatherInfos(fused, ctx)

            coVerify(timeout = 1500) { repository.getCurrentWeather(manualLoc.lat, manualLoc.lon) }
            coVerify(timeout = 1500) {
                repository.getWeatherForecasts(
                    manualLoc.lat,
                    manualLoc.lon
                )
            }

            verify(exactly = 0) { fused.lastLocation }

            val state = vm.weatherState.value
            assert(state.location.name == "Milano")
            assert(state.conditions.any { it.isCurrent })
            assert(state.conditions.any { !it.isCurrent })
            assert(!vm.isRefreshing)
        }

    private fun apiCurrentWeather(
        name: String,
        lat: String,
        lon: String,
        country: String,
        dt: Date
    ): APIWeatherCondition {
        return APIWeatherCondition(
            _weather = listOf(
                APIWeather(
                    main = "Clear",
                    _id = "800",
                    icon = "01d",
                    description = "clear sky"
                )
            ),
            coord = APICoord(
                lat = lat,
                lon = lon
            ),
            main = APIMain(
                _temp = "25.0",
                _feels_like = "25.0",
                _temp_min = "22.0",
                _temp_max = "27.0",
                _pressure = "1012.0",
                humidity = 50
            ),
            wind = APIWind(
                _speed = "3.5"
            ),
            sys = APISys(
                country = country,
                sunrise = Date(dt.time - 3_600_000),
                sunset = Date(dt.time + 3_600_000)
            ),
            name = name,
            dt = dt
        )
    }

    private fun apiForecasts(items: List<APIForecast>): APIWeatherForecasts {
        return APIWeatherForecasts(list = items)
    }

    private fun apiForecastItem(
        dt: Date,
        temp: Int,
        pressure: Int,
        icon: String,
        condId: Int,
        humidity: Int = 60,
        windMs: String = "4.0"
    ): APIForecast {
        return APIForecast(
            dt = dt,
            main = APIMain(
                _temp = temp.toString(),
                _feels_like = temp.toString(),
                _temp_min = temp.toString(),
                _temp_max = temp.toString(),
                _pressure = pressure.toString(),
                humidity = humidity
            ),
            _weather = listOf(
                APIWeather(
                    main = "X",
                    _id = condId.toString(),
                    icon = icon,
                    description = "Y"
                )
            ),
            wind = APIWind(
                _speed = windMs
            )
        )
    }
}