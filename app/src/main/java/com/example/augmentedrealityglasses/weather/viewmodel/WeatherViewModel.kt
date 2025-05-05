package com.example.augmentedrealityglasses.weather.viewmodel

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
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
import com.example.augmentedrealityglasses.weather.constants.Constants
import com.example.augmentedrealityglasses.weather.network.ResultWrapper
import com.example.augmentedrealityglasses.weather.network.WeatherRepositoryImpl
import com.example.augmentedrealityglasses.weather.state.Coord
import com.example.augmentedrealityglasses.weather.state.Main
import com.example.augmentedrealityglasses.weather.state.Sys
import com.example.augmentedrealityglasses.weather.state.Weather
import com.example.augmentedrealityglasses.weather.state.WeatherCondition
import com.example.augmentedrealityglasses.weather.state.WeatherForecasts
import com.example.augmentedrealityglasses.weather.state.WeatherLocation
import com.example.augmentedrealityglasses.weather.state.WeatherUI
import com.example.augmentedrealityglasses.weather.state.WeatherUiState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WeatherViewModel(
    private val repository: WeatherRepositoryImpl
) : ViewModel() {

    //Initialize the viewModel
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val weatherAPIRepository =
                    (this[APPLICATION_KEY] as App).container.weatherAPIRepository
                WeatherViewModel(
                    repository = weatherAPIRepository
                )
            }
        }
    }

    var weatherUI by mutableStateOf(
        WeatherUI(
            "",
            "",
            "",
            "",
            ""
        )
    )
        private set

    //Main UI state
    var weatherState by mutableStateOf(
        WeatherUiState(
            WeatherCondition(
                listOf(Weather("", "")),
                Coord("", ""),
                Main("", ""),
                Sys(""),
                ""
            ),
            WeatherForecasts(
                listOf()
            ),
            "current"
        )
    )
        private set

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

    fun clearSearchedLocationList() {
        _searchedLocations.clear()
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

    private fun updateWeatherState(
        newCurrentWeatherConditions: WeatherCondition = weatherState.currentCondition,
        newForecasts: WeatherForecasts = weatherState.forecasts,
        newTimestamp: String = weatherState.shownTimestamp
    ) {
        weatherState = weatherState.copy(
            currentCondition = newCurrentWeatherConditions,
            forecasts = newForecasts,
            shownTimestamp = newTimestamp
        )

        if (newTimestamp == "current") {
            weatherUI = weatherUI.copy(
                main = weatherState.currentCondition.weather.main,
                description = weatherState.currentCondition.weather.description,
                temp = weatherState.currentCondition.main.temp,
                pressure = weatherState.currentCondition.main.pressure,
                timestamp = "current"
            )
        } else {
            val selectedForecast =
                weatherState.forecasts.list.find { forecast -> forecast.dt == newTimestamp }

            if (selectedForecast != null) {
                weatherUI = weatherUI.copy(
                    main = selectedForecast.weather.main,
                    description = selectedForecast.weather.description,
                    temp = selectedForecast.main.temp,
                    pressure = selectedForecast.main.pressure,
                    timestamp = selectedForecast.dt
                )
            } else {
                //TODO: handle
            }
        }
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
    ): ResultWrapper<WeatherCondition> {
        //FIXME: useful? if (lat.isNotEmpty() && lon.isNotEmpty()) {
//        return try {
//            weatherAPI.getWeatherInfo(
//                lat,
//                lon
//            )
//        } catch (e: IOException) {
//            //network error
//            showErrorMessage("Network error. Please try again later")
//            null
//        } catch (e: HttpException) {
//            //http error
//            showErrorMessage("Something went wrong while fetching the weather conditions. Please try again later")
//            null
//        } catch (e: Exception) {
//            //generic error
//            showErrorMessage("Something went wrong. Please try again later")
//            null
//        }

        return when (val weatherInfo = repository.getCurrentWeatherInfo(lat, lon)) {
            is ResultWrapper.Success -> {
                weatherInfo
            }

            is ResultWrapper.GenericError -> {
                //TODO: use constant
                showErrorMessage("Generic error while fetching weather information. Try again")
                weatherInfo
            }

            is ResultWrapper.NetworkError -> {
                //TODO: use constant
                showErrorMessage("Network error while fetching weather information. Try again")
                weatherInfo
            }
        }
    }

    private suspend fun fetchForecastsInfo(
        lat: String,
        lon: String
    ): ResultWrapper<WeatherForecasts> {
        return when (val forecasts = repository.getWeatherForecast(lat, lon)) {
            is ResultWrapper.Success -> {
                forecasts
            }

            is ResultWrapper.GenericError -> {
                //TODO: use constant
                showErrorMessage("Generic error while fetching weather forecasts information. Try again")
                forecasts
            }

            is ResultWrapper.NetworkError -> {
                //TODO: use constant
                showErrorMessage("Network error while fetching weather forecasts information. Try again")
                forecasts
            }
        }
    }

    private suspend fun fetchLatLonByQuery(query: String): ResultWrapper<List<WeatherLocation>> {
//        return try {
//            weatherAPI.getLocations(
//                query
//            )
//        } catch (e: IOException) {
//            //network error
//            showErrorMessage("Network error. Please try again later")
//            null
//        } catch (e: HttpException) {
//            //http error
//            showErrorMessage("Something went wrong while fetching the locations. Please try again later")
//            null
//        } catch (e: Exception) {
//            //generic error
//            showErrorMessage("Something went wrong. Please try again later")
//            null
//        }

        return when (val locations = repository.searchLocations(query)) {
            is ResultWrapper.Success -> {
                locations
            }

            is ResultWrapper.GenericError -> {
                //TODO: use constant
                showErrorMessage("Generic error while searching locations. Try again")
                locations
            }

            is ResultWrapper.NetworkError -> {
                //TODO: use constant
                showErrorMessage("Network error while searching locations. Try again")
                locations
            }
        }
    }


    @SuppressLint("MissingPermission")
    private suspend fun fetchGeolocation(
        fusedLocationClient: FusedLocationProviderClient,
        context: Context
    ): Location? {
        return suspendCancellableCoroutine { continuation ->
            val permissions = getGeolocationPermissions(context)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { lastLocation: Location? ->
                    if (getGeolocationPermissions(context).values.any { it }) {
                        if (lastLocation != null && (System.currentTimeMillis() - lastLocation.time) <= Constants.MAX_AGE_LAST_LOCATION) {
                            //there is a last location saved and it is not too old
                            isLoading = false
                            continuation.resume(lastLocation)
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
                                else -> throw IllegalStateException("No location permission granted") //TODO: handle
                            }

                            //fetch the current location
                            fusedLocationClient.getCurrentLocation(priority, null)
                                .addOnSuccessListener { currentLocation: Location? ->
                                    isLoading = false

                                    if (currentLocation != null) {
                                        continuation.resume(currentLocation)
                                    } else {
                                        continuation.resume(null)
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    isLoading = false

                                    if (continuation.isActive) {
                                        continuation.resumeWithException(exception)
                                    }
                                }
                        }
                    } else {
                        continuation.resume(null)
                        //TODO: handle
                    }
                }
                .addOnFailureListener { exception ->
                    isLoading = false

                    if (continuation.isActive) {
                        //TODO: handle
                        continuation.resumeWithException(exception)
                    }
                }
        }
    }

    private fun getWeatherByResult(result: WeatherLocation) {
        viewModelScope.launch {
            if (searchedLocations.isNotEmpty()) {

                //get weather infos of the result
                when (val newCurrentWeatherCondition =
                    fetchCurrentWeatherInfo(result.lat, result.lon)) {
                    is ResultWrapper.Success -> {
                        //update weather and location states

                        when (val newForecasts = fetchForecastsInfo(result.lat, result.lon)) {
                            is ResultWrapper.Success -> {

                                updateWeatherState(
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
    }

    // Functions called by the UI (e.g. onClick handlers)

    fun refreshWeatherInfos(
        fusedLocationClient: FusedLocationProviderClient,
        context: Context
    ) {
        viewModelScope.launch {
            if (geolocationEnabled) {
                //fetch geolocation
                //TODO: try only for geolocation (remove it in future maybe)
                try {
                    val geo = fetchGeolocation(fusedLocationClient, context)

                    if (geo != null) {

                        //update weather conditions and location
                        when (val newCurrentWeatherCondition =
                            fetchCurrentWeatherInfo(
                                geo.latitude.toString(),
                                geo.longitude.toString()
                            )) {

                            is ResultWrapper.Success -> {

                                when (val newForecasts = fetchForecastsInfo(
                                    geo.latitude.toString(),
                                    geo.longitude.toString()
                                )) {
                                    is ResultWrapper.Success -> {

                                        updateWeatherState(
                                            newCurrentWeatherCondition.value,
                                            newForecasts.value,
                                            "current" //show current weather
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
                    } else {
                        showErrorMessage("Position unavailable! Please try again")
                    }
                } catch (e: Exception) {
                    //TODO: handle
                    showErrorMessage("An error has occurred while retrieve the geolocation!")
                }
            } else {
                when (val newCurrentWeatherCondition =
                    fetchCurrentWeatherInfo(location.lat, location.lon)) {
                    is ResultWrapper.Success -> {
                        when (val newForecasts = fetchForecastsInfo(location.lat, location.lon)) {
                            is ResultWrapper.Success -> {
                                updateWeatherState(
                                    newCurrentWeatherCondition.value,
                                    newForecasts.value,
                                    "current" //show current weather
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
        viewModelScope.launch {
            //TODO: try only for geolocation (remove it in future maybe)
            try {

                //get geolocation infos
                val geo = fetchGeolocation(fusedLocationClient, context)

                if (geo != null) {
                    //update weather conditions
                    when (val newCurrentWeatherCondition =
                        fetchCurrentWeatherInfo(
                            geo.latitude.toString(),
                            geo.longitude.toString()
                        )) {

                        is ResultWrapper.Success -> {

                            when (val newForecasts = fetchForecastsInfo(
                                geo.latitude.toString(),
                                geo.longitude.toString()
                            )) {
                                is ResultWrapper.Success -> {
                                    updateWeatherState(
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

                } else {
                    showErrorMessage("Position unavailable! Please try again")
                }

            } catch (e: Exception) {
                //TODO: handle
                showErrorMessage("An error has occurred while retrieve the geolocation!")
            }
        }
    }

    fun getWeatherOfFirstResult() {
        getWeatherByResult(searchedLocations[0])
    }

    fun getWeatherOfSelectedLocation(result: WeatherLocation) {
        getWeatherByResult(result)
    }

    fun searchLocations(query: String) {
        viewModelScope.launch {
            when (val locations = fetchLatLonByQuery(query)) {
                is ResultWrapper.Success -> {
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

    fun showCurrentWeather() {
        updateWeatherState(newTimestamp = "current")
    }

    fun showWeatherForecast(timeStamp: String) {
        updateWeatherState(newTimestamp = timeStamp)
    }
}