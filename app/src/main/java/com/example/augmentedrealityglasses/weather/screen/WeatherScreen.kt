package com.example.augmentedrealityglasses.weather.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.augmentedrealityglasses.weather.viewmodel.WeatherViewModel

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = viewModel()
) {
    val uiStateCondition by viewModel.uiState.collectAsStateWithLifecycle()

    val location by remember { derivedStateOf { viewModel.location } }

    var query by remember { mutableStateOf("") }

    Column {
        Row {
            Button(onClick = { updateWeatherInfo(viewModel) }) {
                Text(
                    text = "Update weather info"
                )
            }
            Button(onClick = { }) {
                Text(
                    text = "Geolocation weather"
                )
            }
        }
        Text(
            text = "Location: ${location.getFullName()}"
        )
        Text(
            text = "Latitude: ${location.lat}"
        )
        Text(
            text = "Longitude: ${location.lon}"
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Info: ${uiStateCondition.condition.weather.main}"
        )
        Text(
            text = "Description: ${uiStateCondition.condition.weather.description}"
        )
        Text(
            text = "Temperature: ${uiStateCondition.condition.main.temp}"
        )
        Text(
            text = "Pressure: ${uiStateCondition.condition.main.pressure}"
        )
        Row {
            TextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Query") }
            )
            Button(onClick = { findWeatherByQuery(query, viewModel) }) {
                Text(
                    text = "Search"
                )
            }
        }
        viewModel.searchedLocations.forEach { el ->
            Text(
                text = el.getFullName()
            )
        }
    }
}

fun updateWeatherInfo(viewModel: WeatherViewModel) {
    viewModel.updateInfos()
}

fun findWeatherByQuery(query: String, viewModel: WeatherViewModel) {
    viewModel.findByQuery(query)
}