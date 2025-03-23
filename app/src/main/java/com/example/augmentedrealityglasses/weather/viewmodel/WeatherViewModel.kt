package com.example.augmentedrealityglasses.weather.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.augmentedrealityglasses.weather.network.RetroInstance
import com.example.augmentedrealityglasses.weather.network.RetroService
import com.example.augmentedrealityglasses.weather.state.Location
import com.example.augmentedrealityglasses.weather.state.Main
import com.example.augmentedrealityglasses.weather.state.Weather
import com.example.augmentedrealityglasses.weather.state.WeatherCondition
import com.example.augmentedrealityglasses.weather.state.WeatherUiState
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

    var location by mutableStateOf(Location("45.27", "9.09", "", false))
        private set

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

    fun updateLocation(query: String, gpsLocation: Boolean) {
        location = location.copy(location.lat, location.lon, query, gpsLocation)
    }

    fun findByQuery() {
        viewModelScope.launch(Dispatchers.IO) {
            val retroInstance = RetroInstance.getRetroInstance().create(RetroService::class.java)
            val response = retroInstance.getLatLon(
                location.query
            )

            location.query = response[0].name
            location.lat = response[0].lat
            location.lon = response[0].lon

            updateInfos()
        }
    }
}