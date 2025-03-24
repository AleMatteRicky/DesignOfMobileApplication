package com.example.augmentedrealityglasses.weather.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.augmentedrealityglasses.weather.viewmodel.WeatherViewModel

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = viewModel()
) {
    val uiStateCondition by viewModel.uiState.collectAsStateWithLifecycle()

    val location by remember { derivedStateOf { viewModel.location } }

    Column {
        Button(onClick = { updateWeatherInfo(viewModel) }) {
            Text(
                text = "Update weather info"
            )
        }
        Text(
            text = "Latitude: ${location.lat}"
        )
        Text(
            text = "Longitude: ${location.lon}"
        )
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
                value = location.name,
                onValueChange = { viewModel.updateLocation(it) },
                label = { Text("Query") }
            )
            Button(onClick = { findWeatherByQuery(viewModel) }) {
                Text(
                    text = "Search"
                )
            }
        }
        Text(
            text = "Query: ${location.name}"
        )
    }
}

fun updateWeatherInfo(viewModel: WeatherViewModel) {
    viewModel.updateInfos()
}

fun findWeatherByQuery(viewModel: WeatherViewModel) {
    viewModel.findByQuery()
}