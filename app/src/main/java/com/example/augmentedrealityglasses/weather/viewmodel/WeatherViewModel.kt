package com.example.augmentedrealityglasses.weather.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    private val _uiState = MutableStateFlow(
        WeatherUiState(
            WeatherCondition(
                listOf<Weather>(Weather("null", "null")),
                Main("null", "null"),
                Sys(""),
                ""
            )
        )
    )

    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    var location by mutableStateOf(WeatherLocation("", "45.27", "9.09", "", ""))
        private set

    private var _searchedLocations = mutableStateListOf<WeatherLocation>()

    val searchedLocations: List<WeatherLocation> get() = _searchedLocations


    //business logic functions
    fun updateInfos(
        lat: String = location.lat,
        lon: String = location.lon,
        state: String = location.state.orEmpty(),
        geolocationEnabled: MutableState<Boolean>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val retroInstance = RetroInstance.getRetroInstance().create(RetroService::class.java)
            val response = retroInstance.getWeatherInfo(
                lat,
                lon
            )

            _uiState.update { currentState ->
                currentState.copy(
                    condition = response
                )
            }

            if (geolocationEnabled.value) {
                location = location.copy(
                    name = response.name,
                    lat = lat,
                    lon = lon,
                    country = response.sys.country,
                    state = state
                )
            } else {
                location = location.copy(
                    name = location.name,
                    lat = lat,
                    lon = lon,
                    country = response.sys.country,
                    state = state
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation(
        context: Context,
        fusedLocationClient: FusedLocationProviderClient,
        geolocationEnabled: MutableState<Boolean>
    ) {
        fusedLocationClient.lastLocation.addOnSuccessListener { fetchedLocation: Location? ->
            if (fetchedLocation != null) {

                val lat = fetchedLocation.latitude.toString()
                val lon = fetchedLocation.longitude.toString()

                //call weather API
                updateInfos(lat, lon, "", geolocationEnabled)

                geolocationEnabled.value = true
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

            //FIXME: really useful this check?
            if (response.isNotEmpty()) {

                _searchedLocations.clear()
                _searchedLocations.addAll(response)
            } //TODO: handle the case of empty response
        }
    }

    fun findWeatherByLocation(loc: WeatherLocation, geolocationEnabled: MutableState<Boolean>) {
        location = loc
        updateInfos(geolocationEnabled = geolocationEnabled)
        geolocationEnabled.value = false
    }

    fun findFirstResultWeather(geolocationEnabled: MutableState<Boolean>) {
        if (searchedLocations.isNotEmpty()) {
            location = searchedLocations[0]
            updateInfos(geolocationEnabled = geolocationEnabled)
            geolocationEnabled.value = false
        }
    }
}