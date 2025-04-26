package com.example.augmentedrealityglasses.weather.viewmodel

import android.annotation.SuppressLint
import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.augmentedrealityglasses.weather.constants.Constants
import com.example.augmentedrealityglasses.weather.network.RetroInstance
import com.example.augmentedrealityglasses.weather.network.RetroService
import com.example.augmentedrealityglasses.weather.state.Coord
import com.example.augmentedrealityglasses.weather.state.Main
import com.example.augmentedrealityglasses.weather.state.Sys
import com.example.augmentedrealityglasses.weather.state.Weather
import com.example.augmentedrealityglasses.weather.state.WeatherCondition
import com.example.augmentedrealityglasses.weather.state.WeatherLocation
import com.example.augmentedrealityglasses.weather.state.WeatherUiState
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WeatherViewModel : ViewModel() {

    //Main UI state
    private val _weatherState = MutableStateFlow(
        WeatherUiState(
            WeatherCondition(
                listOf(Weather("", "")),
                Coord("", ""),
                Main("", ""),
                Sys(""),
                ""
            )
        )
    )
    val weatherState: StateFlow<WeatherUiState> = _weatherState.asStateFlow()

    //Selected location to display the weather conditions for
    var location by mutableStateOf(
        WeatherLocation(
            "",
            Constants.INITIAL_VALUE,
            Constants.INITIAL_VALUE,
            "",
            ""
        )
    )
        private set

    //List of all the locations found by the API (by specifying a query)
    private var _searchedLocations = mutableStateListOf<WeatherLocation>()
    val searchedLocations: List<WeatherLocation> get() = _searchedLocations

    //Geolocation Permissions
    var hasCoarseLocationPermission by mutableStateOf(false)
        private set
    var hasFineLocationPermission by mutableStateOf(false)
        private set

    //Geolocation state
    var geolocationEnabled by mutableStateOf(false)
        private set

    //For managing the visibility of the Text "no results found"
    var showNoResults by mutableStateOf(false)
        private set

    //Interface for the API
    private val retroInstance = RetroInstance.getRetroInstance().create(RetroService::class.java)

    //Error message
    private val _errorVisible = MutableStateFlow(false)
    val errorVisible: StateFlow<Boolean> = _errorVisible

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    // LOGIC FUNCTIONS

    fun clearSearchedLocationList() {
        _searchedLocations.clear()
    }

    private fun updateWeatherState(newWeatherCondition: WeatherCondition) {
        _weatherState.update { currentState ->
            currentState.copy(
                condition = newWeatherCondition
            )
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

    fun setGeolocationPermissions(coarse: Boolean, fine: Boolean) {
        hasCoarseLocationPermission = coarse
        hasFineLocationPermission = fine
    }

    fun hideNoResult() {
        showNoResults = false
    }

    private fun showErrorMessage(errorMsg: String) {
        _errorMessage.value = errorMsg
        _errorVisible.value = true
    }

    fun hideErrorMessage() {
        _errorVisible.value = false
    }

//    fun updateInfos(
//        loc: WeatherLocation = location,
//        enabled: Boolean = geolocationEnabled
//    ) {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                if (loc.lat != Constants.INITIAL_VALUE && loc.lon != Constants.INITIAL_VALUE) {
//                    val response = retroInstance.getWeatherInfo(
//                        loc.lat,
//                        loc.lon
//                    )
//
//                    _weatherState.update { currentState ->
//                        currentState.copy(
//                            condition = response
//                        )
//                    }
//
//                    geolocationEnabled = enabled
//
//                    if (geolocationEnabled) {
//                        location = location.copy(
//                            name = response.name,
//                            lat = loc.lat,
//                            lon = loc.lon,
//                            country = response.sys.country,
//                            state = ""
//                        )
//                    } else {
//                        location = location.copy(
//                            name = loc.name,
//                            lat = loc.lat,
//                            lon = loc.lon,
//                            country = response.sys.country,
//                            state = loc.state.orEmpty()
//                        )
//                    }
//                }
//            } catch (e: IOException) {
//                //network error
//                showErrorMessage("Network error. Please try again later")
//            } catch (e: HttpException) {
//                //http error
//                showErrorMessage("Something went wrong while fetching the weather conditions. Please try again later")
//            } catch (e: Exception) {
//                //generic error
//                showErrorMessage("Something went wrong. Please try again later")
//            }
//        }
//    }

//    @SuppressLint("MissingPermission")
//    fun fetchCurrentLocation(
//        context: Context,
//        fusedLocationClient: FusedLocationProviderClient,
//    ) {
//        fusedLocationClient.lastLocation.addOnSuccessListener { fetchedLocation: Location? ->
//            if (fetchedLocation != null) {
//
//                val lat = fetchedLocation.latitude.toString()
//                val lon = fetchedLocation.longitude.toString()
//
//                //call weather API
//                updateInfos(
//                    WeatherLocation("", lat, lon, "", ""),
//                    enabled = true
//                )
//            } else {
//                Toast.makeText(context, "Current position unavailable", Toast.LENGTH_SHORT)
//                    .show()
//            }
//        }
//    }

//    fun findLocationsByQuery(query: String) {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                val response = retroInstance.getLatLon(
//                    query
//                )
//
//                _searchedLocations.clear()
//                _searchedLocations.addAll(response)
//
//                showNoResults = true
//
//            } catch (e: IOException) {
//                //network error
//                clearSearchedLocationList()
//                showErrorMessage("Network error. Please try again later")
//            } catch (e: HttpException) {
//                //http error
//                clearSearchedLocationList()
//                showErrorMessage("Something went wrong while fetching the weather conditions. Please try again later")
//            } catch (e: Exception) {
//                //generic error
//                clearSearchedLocationList()
//                showErrorMessage("Something went wrong. Please try again later")
//            }
//        }
//    }

//    fun findWeatherInfosByLocation(
//        loc: WeatherLocation
//    ) {
//        updateInfos(loc, enabled = false)
//    }

//    fun getWeatherOfFirstResult() {
//        if (searchedLocations.isNotEmpty()) {
//            updateInfos(
//                loc = searchedLocations[0],
//                enabled = false
//            )
//        }
//    }

//    fun updateWeatherInfos(
//        context: Context,
//        requestPermissionsLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
//        fusedLocationClient: FusedLocationProviderClient
//    ) {
//        if (geolocationEnabled) {
//            getGeolocation(
//                context,
//                requestPermissionsLauncher,
//                fusedLocationClient
//            )
//        } else {
//            updateInfos()
//        }
//    }

//    fun getGeolocation(
//        context: Context,
//        requestPermissionsLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>,
//        fusedLocationClient: FusedLocationProviderClient
//    ) {
//        when {
//            hasCoarseLocationPermission || hasFineLocationPermission -> {
//
//                //Fetch the position
//                fetchCurrentLocation(context, fusedLocationClient)
//            }
//
//            ActivityCompat.shouldShowRequestPermissionRationale(
//                context as Activity,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            )
//                    || ActivityCompat.shouldShowRequestPermissionRationale(
//                context,
//                Manifest.permission.ACCESS_COARSE_LOCATION
//            ) -> {
//                Toast.makeText(context, "Explain", Toast.LENGTH_SHORT).show()
//
//                requestPermissionsLauncher.launch(
//                    arrayOf(
//                        Manifest.permission.ACCESS_COARSE_LOCATION,
//                        Manifest.permission.ACCESS_FINE_LOCATION
//                    )
//                )
//            }
//
//            else -> {
//                requestPermissionsLauncher.launch(
//                    arrayOf(
//                        Manifest.permission.ACCESS_COARSE_LOCATION,
//                        Manifest.permission.ACCESS_FINE_LOCATION
//                    )
//                )
//            }
//        }
//    }

//    fun getGeolocationWeather(
//        context: Context,
//        requestPermissionsLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
//        fusedLocationClient: FusedLocationProviderClient
//    ) {
//        getGeolocation(
//            context,
//            requestPermissionsLauncher,
//            fusedLocationClient
//        )
//    }

    private suspend fun fetchWeatherInfo(lat: String, lon: String): WeatherCondition? {
        return try {
            retroInstance.getWeatherInfo(
                lat,
                lon
            )
        } catch (e: IOException) {
            //network error
            showErrorMessage("Network error. Please try again later")
            null
        } catch (e: HttpException) {
            //http error
            showErrorMessage("Something went wrong while fetching the weather conditions. Please try again later")
            null
        } catch (e: Exception) {
            //generic error
            showErrorMessage("Something went wrong. Please try again later")
            null
        }
    }

    private suspend fun fetchLatLonByQuery(query: String): List<WeatherLocation>? {
        return try {
            retroInstance.getLocations(
                query
            )
        } catch (e: IOException) {
            //network error
            showErrorMessage("Network error. Please try again later")
            null
        } catch (e: HttpException) {
            //http error
            showErrorMessage("Something went wrong while fetching the locations. Please try again later")
            null
        } catch (e: Exception) {
            //generic error
            showErrorMessage("Something went wrong. Please try again later")
            null
        }
    }

    @SuppressLint("MissingPermission") //FIXME
    private suspend fun fetchGeolocation(
        fusedLocationClient: FusedLocationProviderClient
    ): Location? {
        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        continuation.resume(location)
                    } else {
                        //TODO: handle
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener { exception ->
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
                val newWeatherCondition = fetchWeatherInfo(result.lat, result.lon)

                if (newWeatherCondition != null) {

                    //update weather and location states
                    updateWeatherState(newWeatherCondition)

                    updateLocationState(
                        newWeatherCondition.name,
                        newWeatherCondition.coord.lat,
                        newWeatherCondition.coord.lon,
                        newWeatherCondition.sys.country,
                        result.state.orEmpty()
                    )

                    //disable geolocationEnabled
                    geolocationEnabled = false
                }
            }
        }
    }

    // Functions called by the UI (e.g. onClick handlers)

    fun refreshWeatherInfos(fusedLocationClient: FusedLocationProviderClient) {
        viewModelScope.launch {
            if (geolocationEnabled) {
                //fetch geolocation
                try {
                    val geo = fetchGeolocation(fusedLocationClient)

                    if (geo != null) {

                        //update weather conditions and location
                        val newWeatherCondition =
                            fetchWeatherInfo(geo.latitude.toString(), geo.longitude.toString())

                        if (newWeatherCondition != null) {
                            updateWeatherState(newWeatherCondition)

                            // state not available with this API call
                            updateLocationState(
                                newWeatherCondition.name,
                                newWeatherCondition.coord.lat,
                                newWeatherCondition.coord.lon,
                                newWeatherCondition.sys.country,
                                ""
                            )
                        }
                    } else {
                        showErrorMessage("Position unavailable! Please try again")
                    }
                } catch (e: Exception) {
                    //TODO: handle
                    showErrorMessage("An error has occurred!")
                }
            } else {
                val newWeatherCondition = fetchWeatherInfo(location.lat, location.lon)

                if (newWeatherCondition != null) {
                    updateWeatherState(newWeatherCondition)
                }
            }
        }
    }

    fun getGeolocationWeather(fusedLocationClient: FusedLocationProviderClient) {
        viewModelScope.launch {
            try {

                //get geolocation infos
                val geo = fetchGeolocation(fusedLocationClient)

                //update weather conditions
                if (geo != null) {
                    val newWeatherCondition =
                        fetchWeatherInfo(geo.latitude.toString(), geo.longitude.toString())

                    if (newWeatherCondition != null) {
                        updateWeatherState(newWeatherCondition)

                        // state not available with this API call
                        updateLocationState(
                            newWeatherCondition.name,
                            newWeatherCondition.coord.lat,
                            newWeatherCondition.coord.lon,
                            newWeatherCondition.sys.country,
                            ""
                        )

                        geolocationEnabled = true
                    }

                } else {
                    showErrorMessage("Position unavailable! Please try again")
                }

            } catch (e: Exception) {
                //TODO: handle
                showErrorMessage("An error has occurred!")
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
            val locations = fetchLatLonByQuery(query)

            if (locations != null) {
                _searchedLocations.clear()
                _searchedLocations.addAll(locations)

                showNoResults = true
            }
        }
    }
}