package com.example.augmentedrealityglasses.weather.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.augmentedrealityglasses.weather.network.RetroInstance
import com.example.augmentedrealityglasses.weather.network.RetroService
import com.example.augmentedrealityglasses.weather.state.Main
import com.example.augmentedrealityglasses.weather.state.Sys
import com.example.augmentedrealityglasses.weather.state.Weather
import com.example.augmentedrealityglasses.weather.state.WeatherCondition
import com.example.augmentedrealityglasses.weather.state.WeatherLocation
import com.example.augmentedrealityglasses.weather.state.WeatherUiState
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WeatherViewModel : ViewModel() {

    //Main UI state
    private val _weatherState = MutableStateFlow(
        WeatherUiState(
            WeatherCondition(
                listOf(Weather("", "")),
                Main("", ""),
                Sys(""),
                ""
            )
        )
    )
    val weatherState: StateFlow<WeatherUiState> = _weatherState.asStateFlow()

    //Selected location to display the weather conditions for
    var location by mutableStateOf(WeatherLocation("", "0", "0", "", ""))
        private set

    //List of all the locations found by the API
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

    //Logic functions

    fun setGeolocationPermissions(coarse: Boolean, fine: Boolean) {
        hasCoarseLocationPermission = coarse
        hasFineLocationPermission = fine
    }

    fun setShowNoResultsState(show: Boolean) {
        showNoResults = show
    }

    fun updateInfos(
        loc: WeatherLocation = location,
        enabled: Boolean = geolocationEnabled
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val retroInstance = RetroInstance.getRetroInstance().create(RetroService::class.java)
            val response = retroInstance.getWeatherInfo(
                loc.lat,
                loc.lon
            )

            _weatherState.update { currentState ->
                currentState.copy(
                    condition = response
                )
            }

            geolocationEnabled = enabled

            if (geolocationEnabled) {
                location = location.copy(
                    name = response.name,
                    lat = loc.lat,
                    lon = loc.lon,
                    country = response.sys.country,
                    state = ""
                )
            } else {
                location = location.copy(
                    name = loc.name,
                    lat = loc.lat,
                    lon = loc.lon,
                    country = response.sys.country,
                    state = loc.state.orEmpty()
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation(
        context: Context,
        fusedLocationClient: FusedLocationProviderClient,
    ) {
        fusedLocationClient.lastLocation.addOnSuccessListener { fetchedLocation: Location? ->
            if (fetchedLocation != null) {

                val lat = fetchedLocation.latitude.toString()
                val lon = fetchedLocation.longitude.toString()

                //call weather API
                updateInfos(
                    WeatherLocation("", lat, lon, "", ""),
                    enabled = true
                )
            } else {
                Toast.makeText(context, "Current position unavailable", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    fun clearSearchedLocationList() {
        _searchedLocations.clear()
    }

    fun findLocationsByQuery(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val retroInstance = RetroInstance.getRetroInstance().create(RetroService::class.java)
            val response = retroInstance.getLatLon(
                query
            )

            _searchedLocations.clear()
            _searchedLocations.addAll(response)

            showNoResults = true
        }
    }

    fun findWeatherInfosByLocation(
        loc: WeatherLocation
    ) {
        updateInfos(loc, enabled = false)
    }

    fun getWeatherOfFirstResult() {
        if (searchedLocations.isNotEmpty()) {
            updateInfos(
                loc = searchedLocations[0],
                enabled = false
            )
            geolocationEnabled = false
        }
    }

    fun updateWeatherInfos(
        context: Context,
        requestPermissionsLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
        fusedLocationClient: FusedLocationProviderClient
    ) {
        if (geolocationEnabled) {
            getGeolocation(
                context,
                requestPermissionsLauncher,
                fusedLocationClient
            )
        } else {
            updateInfos()
        }
    }

    fun getGeolocation(
        context: Context,
        requestPermissionsLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>,
        fusedLocationClient: FusedLocationProviderClient
    ) {
        when {
            hasCoarseLocationPermission || hasFineLocationPermission -> {

                //Fetch the position
                fetchCurrentLocation(context, fusedLocationClient)
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                context as Activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
                    || ActivityCompat.shouldShowRequestPermissionRationale(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) -> {
                Toast.makeText(context, "Explain", Toast.LENGTH_SHORT).show()

                requestPermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            }

            else -> {
                requestPermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            }
        }
    }

    fun getGeolocationWeather(
        context: Context,
        requestPermissionsLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
        fusedLocationClient: FusedLocationProviderClient
    ) {
        getGeolocation(
            context,
            requestPermissionsLauncher,
            fusedLocationClient
        )
    }
}