package com.example.augmentedrealityglasses.weather.viewmodel

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.augmentedrealityglasses.App
import com.example.augmentedrealityglasses.ble.device.RemoteDeviceManager
import com.example.augmentedrealityglasses.weather.constants.Constants
import com.example.augmentedrealityglasses.weather.network.APIResult
import com.example.augmentedrealityglasses.weather.network.APIWeatherCondition
import com.example.augmentedrealityglasses.weather.network.APIWeatherForecasts
import com.example.augmentedrealityglasses.weather.network.WeatherRepositoryImpl
import com.example.augmentedrealityglasses.weather.state.GeolocationResult
import com.example.augmentedrealityglasses.weather.state.WeatherCondition
import com.example.augmentedrealityglasses.weather.state.WeatherLocation
import com.example.augmentedrealityglasses.weather.state.WeatherUiState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Date
import kotlin.coroutines.resume

class WeatherViewModel(
    private val repository: WeatherRepositoryImpl,
    private val bleManager: RemoteDeviceManager
) : ViewModel() {

    //Initialize the viewModel
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val weatherAPIRepository =
                    (this[APPLICATION_KEY] as App).container.weatherAPIRepository
                val bleManager =
                    (this[APPLICATION_KEY] as App).container.bleManager
                WeatherViewModel(
                    repository = weatherAPIRepository,
                    bleManager = bleManager
                )
            }
        }
    }

    // Start listening for Bluetooth packets
    init {
        viewModelScope.launch {
            try {
                bleManager.receiveUpdates()
                    .collect { connectionState ->
                        isExtDeviceConnected =
                            connectionState.connectionState == BluetoothProfile.STATE_CONNECTED
                    }
            } catch (_: IllegalArgumentException) {

            }
        }
    }

    //Tag for logging
    private val TAG = "weather_viewModel"

    //Main UI state
    private var weatherState by mutableStateOf(
        WeatherUiState(
            listOf()
        )
    )

    // Tracks the Bluetooth connection status with the external device
    var isExtDeviceConnected by mutableStateOf(false)
        private set

    //Day shown
    //TODO: change name
    var dayShown by mutableStateOf(Date())

    //Selected location to display the weather conditions for
    var location by mutableStateOf(
        WeatherLocation(
            "",
            "",
            "",
            "",
            ""
        )
    )
        private set

    //List of all the locations found by the API (by specifying a query)
    private var _searchedLocations = mutableStateListOf<WeatherLocation>()
    val searchedLocations: List<WeatherLocation> get() = _searchedLocations

    //Geolocation state
    var geolocationEnabled by mutableStateOf(false)
        private set

    //For managing the visibility of the Text "no results found"
    var showNoResults by mutableStateOf(false)
        private set

    //Error message
    var errorVisible by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf("")
        private set

    //Loading screen
    var isLoading by mutableStateOf(false)

    //Input for searching the location
    var query by mutableStateOf("")

    // LOGIC FUNCTIONS

    fun updateQuery(newQuery: String) {
        query = newQuery
    }

    fun clearSearchedLocationList() {
        _searchedLocations.clear()
    }

    private fun sendBluetoothMessage(msg: String) {
        if (isExtDeviceConnected) {
            bleManager.send(msg)
        } else {
            Log.d(TAG, "External device not connected")
        }
    }

    fun getGeolocationPermissions(context: Context): Map<String, Boolean> {
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(
            context,
            ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return mapOf(
            Pair(ACCESS_COARSE_LOCATION, hasCoarseLocationPermission), Pair(
                ACCESS_FINE_LOCATION, hasFineLocationPermission
            )
        )
    }

    private fun updateConditionsAndDateToShow(
        newCurrentCondition: APIWeatherCondition,
        newForecasts: APIWeatherForecasts
    ) {
        val newConditions: List<WeatherCondition> = listOf(
            WeatherCondition(
                newCurrentCondition.weather.main,
                newCurrentCondition.weather.description,
                newCurrentCondition.weather.id,
                newCurrentCondition.main.temp,
                newCurrentCondition.main.feels_like,
                newCurrentCondition.main.temp_min,
                newCurrentCondition.main.temp_max,
                newCurrentCondition.main.pressure,
                newCurrentCondition.dt,
                true
            )
        ) +
                newForecasts.list.map { forecast ->
                    WeatherCondition(
                        forecast.weather.main,
                        forecast.weather.description,
                        forecast.weather.id,
                        forecast.main.temp,
                        forecast.main.feels_like,
                        forecast.main.temp_min,
                        forecast.main.temp_max,
                        forecast.main.pressure,
                        forecast.dt,
                        false
                    )
                }

        weatherState = weatherState.copy(
            conditions = newConditions
        )

        changeDay(getMinDateOfAvailableForecasts())

        //send updates to ESP (just the current condition)
        sendBluetoothMessage(newConditions.first { cond -> cond.isCurrent }.toString())
    }

    fun changeDay(newDate: Date) {
        dayShown = newDate
    }

    private fun getMinDateOfAvailableForecasts(): Date {
        val date = getForecasts().map { condition -> condition.dateTime }.toList().minOrNull()

        if (date != null) {
            return date
        }
        Log.d(TAG, "no conditions in the list")
        throw IllegalStateException()
    }

    private fun updateLocationState(
        name: String,
        lat: String,
        lon: String,
        country: String,
        state: String
    ) {
        location = location.copy(
            name = name,
            lat = lat,
            lon = lon,
            country = country,
            state = state
        )

        //send updates to the ESP
        sendBluetoothMessage(location.toString())
    }

    fun hideNoResult() {
        showNoResults = false
    }

    private fun showErrorMessage(errorMsg: String) {
        errorMessage = errorMsg
        errorVisible = true
    }

    fun hideErrorMessage() {
        errorVisible = false
    }

    private suspend fun fetchCurrentWeatherInfo(
        lat: String,
        lon: String
    ): APIResult<APIWeatherCondition> {
        return when (val weatherInfo = repository.getCurrentWeather(lat, lon)) {
            is APIResult.Success -> {
                weatherInfo
            }

            is APIResult.GenericError -> {
                Log.d(TAG, "Error (code ${weatherInfo.code}): ${weatherInfo.error}")
                showErrorMessage(Constants.ERROR_GENERIC_CURRENT_WEATHER)
                weatherInfo
            }

            is APIResult.NetworkError -> {
                Log.d(TAG, "Network error")
                showErrorMessage(Constants.ERROR_NETWORK_CURRENT_WEATHER)
                weatherInfo
            }
        }
    }

    private suspend fun fetchForecastsInfo(
        lat: String,
        lon: String
    ): APIResult<APIWeatherForecasts> {
        return when (val forecasts = repository.getWeatherForecasts(lat, lon)) {
            is APIResult.Success -> {
                forecasts
            }

            is APIResult.GenericError -> {
                Log.d(TAG, "Error (code ${forecasts.code}): ${forecasts.error}")
                showErrorMessage(Constants.ERROR_GENERIC_FORECASTS)
                forecasts
            }

            is APIResult.NetworkError -> {
                Log.d(TAG, "Network error")
                showErrorMessage(Constants.ERROR_NETWORK_FORECASTS)
                forecasts
            }
        }
    }

    private suspend fun fetchLatLonByQuery(query: String): APIResult<List<WeatherLocation>> {
        return when (val locations = repository.searchLocations(query)) {
            is APIResult.Success -> {
                locations
            }

            is APIResult.GenericError -> {
                Log.d(TAG, "Error (code ${locations.code}): ${locations.error}")
                showErrorMessage(Constants.ERROR_GENERIC_LOCATIONS)
                locations
            }

            is APIResult.NetworkError -> {
                Log.d(TAG, "Network error")
                showErrorMessage(Constants.ERROR_NETWORK_LOCATIONS)
                locations
            }
        }
    }


    @SuppressLint("MissingPermission")
    private suspend fun fetchGeolocation(
        fusedLocationClient: FusedLocationProviderClient,
        context: Context
    ): GeolocationResult {
        if (getGeolocationPermissions(context).values.none { it }) {
            return GeolocationResult.NoPermissionGranted
        }

        return suspendCancellableCoroutine { continuation ->
            val permissions = getGeolocationPermissions(context)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { lastLocation: Location? ->

                    if (lastLocation != null && (System.currentTimeMillis() - lastLocation.time) <= Constants.MAX_AGE_LAST_LOCATION) {
                        //there is a last location saved and it is not too old
                        isLoading = false
                        continuation.resume(GeolocationResult.Success(lastLocation))
                    } else {
                        isLoading = true
                        val priority: Int = when {
                            permissions.getOrDefault(
                                ACCESS_FINE_LOCATION,
                                false
                            ) -> Priority.PRIORITY_HIGH_ACCURACY

                            permissions.getOrDefault(
                                ACCESS_COARSE_LOCATION,
                                false
                            ) -> Priority.PRIORITY_BALANCED_POWER_ACCURACY //FIXME: loading is too long
                            else -> {
                                continuation.resume(GeolocationResult.NoPermissionGranted)
                                return@addOnSuccessListener
                            }
                        }

                        //fetch the current location
                        fusedLocationClient.getCurrentLocation(priority, null)
                            .addOnSuccessListener { currentLocation: Location? ->
                                isLoading = false

                                if (currentLocation != null) {
                                    continuation.resume(
                                        GeolocationResult.Success(
                                            currentLocation
                                        )
                                    )
                                } else {
                                    continuation.resume(GeolocationResult.NotAvailable)
                                }
                            }
                            .addOnFailureListener { exception ->
                                isLoading = false

                                if (continuation.isActive) {
                                    continuation.resume(GeolocationResult.Error(exception))
                                }
                            }
                    }

                }
                .addOnFailureListener { exception ->
                    isLoading = false

                    if (continuation.isActive) {
                        continuation.resume(GeolocationResult.Error(exception))
                    }
                }
        }
    }

    private fun getWeatherByResult(result: WeatherLocation) {
        viewModelScope.launch {

            //get weather infos of the result
            when (val newCurrentWeatherCondition =
                fetchCurrentWeatherInfo(result.lat, result.lon)) {
                is APIResult.Success -> {
                    //update weather and location states

                    when (val newForecasts = fetchForecastsInfo(result.lat, result.lon)) {
                        is APIResult.Success -> {

                            updateConditionsAndDateToShow(
                                newCurrentWeatherCondition.value,
                                newForecasts.value
                            )

                            updateLocationState(
                                newCurrentWeatherCondition.value.name,
                                newCurrentWeatherCondition.value.coord.lat,
                                newCurrentWeatherCondition.value.coord.lon,
                                newCurrentWeatherCondition.value.sys.country,
                                result.state.orEmpty()
                            )

                            //disable geolocationEnabled
                            geolocationEnabled = false

                        }

                        else -> {
                            //all the other cases already handled
                        }
                    }
                }

                else -> {
                    //all the other cases already handled
                }
            }
        }
    }

// Functions called by the UI (e.g. onClick handlers)

    fun refreshWeatherInfos(
        fusedLocationClient: FusedLocationProviderClient,
        context: Context
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (geolocationEnabled) {

                //fetch geolocation
                //FIXME: run this in Dispatchers.IO?
                when (val geo = fetchGeolocation(fusedLocationClient, context)) {

                    is GeolocationResult.Success -> {

                        //update weather conditions and location
                        when (val newCurrentWeatherCondition =
                            fetchCurrentWeatherInfo(
                                geo.data.latitude.toString(),
                                geo.data.longitude.toString()
                            )) {

                            is APIResult.Success -> {

                                when (val newForecasts = fetchForecastsInfo(
                                    geo.data.latitude.toString(),
                                    geo.data.longitude.toString()
                                )) {
                                    is APIResult.Success -> {

                                        updateConditionsAndDateToShow(
                                            newCurrentWeatherCondition.value,
                                            newForecasts.value
                                        )

                                        // state not available with this API call
                                        updateLocationState(
                                            newCurrentWeatherCondition.value.name,
                                            newCurrentWeatherCondition.value.coord.lat,
                                            newCurrentWeatherCondition.value.coord.lon,
                                            newCurrentWeatherCondition.value.sys.country,
                                            ""
                                        )
                                    }

                                    else -> {
                                        //all the other cases already handled
                                    }
                                }
                            }

                            else -> {
                                //all the other cases already handled
                            }
                        }
                    }

                    is GeolocationResult.NotAvailable -> {
                        showErrorMessage(Constants.ERROR_GEOLOCATION_NOT_AVAILABLE)
                    }

                    is GeolocationResult.NoPermissionGranted -> {
                        showErrorMessage(Constants.ERROR_GEOLOCATION_NO_PERMISSIONS)
                    }

                    is GeolocationResult.Error -> {
                        Log.d(TAG, "Error: ${geo.exception.message.orEmpty()}")
                        showErrorMessage(Constants.ERROR_GEOLOCATION_GENERIC)
                    }
                }
            } else {
                when (val newCurrentWeatherCondition =
                    fetchCurrentWeatherInfo(location.lat, location.lon)) {
                    is APIResult.Success -> {
                        when (val newForecasts = fetchForecastsInfo(location.lat, location.lon)) {
                            is APIResult.Success -> {

                                updateConditionsAndDateToShow(
                                    newCurrentWeatherCondition.value,
                                    newForecasts.value,
                                )
                            }

                            else -> {
                                //all the other cases already handled
                            }
                        }
                    }

                    else -> {
                        //all the other cases already handled
                    }
                }
            }
        }
    }

    fun getGeolocationWeather(
        fusedLocationClient: FusedLocationProviderClient,
        context: Context
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            //get geolocation infos
            //FIXME: run this in Dispatchers.IO?
            when (val geo = fetchGeolocation(fusedLocationClient, context)) {

                is GeolocationResult.Success -> {
                    //update weather conditions
                    when (val newCurrentWeatherCondition =
                        fetchCurrentWeatherInfo(
                            geo.data.latitude.toString(),
                            geo.data.longitude.toString()
                        )) {

                        is APIResult.Success -> {

                            when (val newForecasts = fetchForecastsInfo(
                                geo.data.latitude.toString(),
                                geo.data.longitude.toString()
                            )) {
                                is APIResult.Success -> {

                                    updateConditionsAndDateToShow(
                                        newCurrentWeatherCondition.value,
                                        newForecasts.value
                                    )

                                    // state not available with this API call
                                    updateLocationState(
                                        newCurrentWeatherCondition.value.name,
                                        newCurrentWeatherCondition.value.coord.lat,
                                        newCurrentWeatherCondition.value.coord.lon,
                                        newCurrentWeatherCondition.value.sys.country,
                                        ""
                                    )

                                    geolocationEnabled = true
                                }

                                else -> {
                                    //all the other cases already handled
                                }
                            }
                        }

                        else -> {
                            //all the other cases already handled
                        }
                    }
                }

                is GeolocationResult.NotAvailable -> {
                    showErrorMessage(Constants.ERROR_GEOLOCATION_NOT_AVAILABLE)
                }

                is GeolocationResult.NoPermissionGranted -> {
                    showErrorMessage(Constants.ERROR_GEOLOCATION_NO_PERMISSIONS)
                }

                is GeolocationResult.Error -> {
                    Log.d(TAG, "Error: ${geo.exception.message.orEmpty()}")
                    showErrorMessage(Constants.ERROR_GEOLOCATION_GENERIC)
                }
            }
        }
    }

    fun getWeatherOfFirstResult() {
        getWeatherByResult(searchedLocations[0])
    }

    fun getWeatherOfSelectedLocation(result: WeatherLocation) {
        query = ""
        getWeatherByResult(result)
    }

    fun searchLocations(query: String) {
        viewModelScope.launch {
            when (val locations = fetchLatLonByQuery(query)) {
                is APIResult.Success -> {
                    _searchedLocations.clear()
                    _searchedLocations.addAll(locations.value)

                    showNoResults = true
                }

                else -> {
                    //all the other cases already handled
                }
            }
        }
    }

    fun getCurrentWeather(): WeatherCondition? {
        val weatherInfo = weatherState.conditions.find { condition -> condition.isCurrent }

        return weatherInfo
    }

    fun getForecasts(): List<WeatherCondition> {
        //FIXME: improve this: do not use filter to remove just one element from the list
        return weatherState.conditions.filter { condition -> !condition.isCurrent }
    }
}