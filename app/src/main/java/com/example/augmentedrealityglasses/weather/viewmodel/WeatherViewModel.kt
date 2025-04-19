package com.example.augmentedrealityglasses.weather.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.augmentedrealityglasses.weather.network.RetroInstance
import com.example.augmentedrealityglasses.weather.network.RetroService
import com.example.augmentedrealityglasses.weather.state.Geolocation
import com.example.augmentedrealityglasses.weather.state.Main
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
                Main("null", "null")
            )
        )
    )

    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    var location by mutableStateOf(WeatherLocation("", "45.27", "9.09", "", ""))
        private set

    private var _searchedLocations = mutableStateListOf<WeatherLocation>()

    val searchedLocations: List<WeatherLocation> get() = _searchedLocations

    val geolocation = mutableStateOf(Geolocation("0", "0"))

    //business logic functions
    fun updateInfos() {
        viewModelScope.launch(Dispatchers.IO) {
            val retroInstance = RetroInstance.getRetroInstance().create(RetroService::class.java)
            val response = retroInstance.getWeatherInfo(
                location.lat,
                location.lon
            )

            _uiState.update { currentState ->
                currentState.copy(
                    condition = response,
                )
            }
        }
    }

    fun findByQuery(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val retroInstance = RetroInstance.getRetroInstance().create(RetroService::class.java)
            val response = retroInstance.getLatLon(
                query
            )

            if (response.isNotEmpty()) {
                val firstResult = response[0]

                location = location.copy(
                    name = firstResult.name,
                    lat = firstResult.lat,
                    lon = firstResult.lon,
                    country = firstResult.country,
                    state = firstResult.state
                )

                _searchedLocations.clear()
                _searchedLocations.addAll(response)

                updateInfos()
            } //TODO: handle the case of empty response
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation(context: Context, fusedLocationClient: FusedLocationProviderClient) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                geolocation.value =
                    Geolocation(location.latitude.toString(), location.longitude.toString())
            } else {
                Toast.makeText(context, "Current position unavailable", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}