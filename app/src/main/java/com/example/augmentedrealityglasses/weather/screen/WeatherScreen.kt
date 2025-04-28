package com.example.augmentedrealityglasses.weather.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.augmentedrealityglasses.weather.constants.Constants
import com.example.augmentedrealityglasses.weather.viewmodel.WeatherViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = viewModel()
) {
    //Context
    val context = LocalContext.current

    val requestPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        //update the state
        viewModel.setGeolocationPermissions(coarseLocationGranted, fineLocationGranted)
    }

    //Client for fetching the geolocation infos
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient((context))
    }

    LaunchedEffect(viewModel.hasCoarseLocationPermission, viewModel.hasFineLocationPermission) {
        if (!viewModel.hasCoarseLocationPermission && !viewModel.hasFineLocationPermission) {
            //request permissions
            requestPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }

        if (viewModel.hasCoarseLocationPermission || viewModel.hasFineLocationPermission) {
            viewModel.getGeolocationWeather(fusedLocationClient)
        }
    }

    var previousQuery by remember { mutableStateOf(viewModel.query) }

    LaunchedEffect(viewModel.query) {
        val isKeyChanged = previousQuery != viewModel.query
        previousQuery = viewModel.query

        if (isKeyChanged) {
            viewModel.hideNoResult()
        }

        if (viewModel.query.isBlank()) {
            viewModel.clearSearchedLocationList()
            return@LaunchedEffect
        }

        delay(Constants.DEBOUNCE_DELAY)
        viewModel.searchLocations(viewModel.query)
    }

    //To make the error message disappear after time
    LaunchedEffect(viewModel.errorVisible) {
        if (viewModel.errorVisible) {
            delay(Constants.ERROR_DISPLAY_TIME)
            viewModel.hideErrorMessage()
        }
    }

    // ----  UI  ----
    Box(modifier = Modifier.fillMaxSize()) {
        if (viewModel.isLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Finding your location...")
            }
        } else {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            viewModel.hideErrorMessage()
                            viewModel.refreshWeatherInfos(fusedLocationClient)
                        },
                        enabled = viewModel.location.lat.isNotEmpty() && viewModel.location.lon.isNotEmpty()
                    ) {
                        Text(
                            text = "Update weather info"
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.query = ""
                            viewModel.hideErrorMessage()
                            viewModel.getGeolocationWeather(fusedLocationClient)
                        },
                        enabled = !viewModel.geolocationEnabled
                    ) {
                        Text(
                            text = "Geolocation weather"
                        )
                    }
                }
                Text(
                    text = if (viewModel.geolocationEnabled) "Current location: ${viewModel.location.getFullName()}" else "Searched location: ${viewModel.location.getFullName()}",
                    color = if (viewModel.geolocationEnabled) Color.Red else Color.Black
                )
                Text(
                    text = "Latitude: ${viewModel.location.lat}"
                )
                Text(
                    text = "Longitude: ${viewModel.location.lon}"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Info: ${viewModel.weatherState.condition.weather.main}"
                )
                Text(
                    text = "Description: ${viewModel.weatherState.condition.weather.description}"
                )
                Text(
                    text = "Temperature: ${viewModel.weatherState.condition.main.temp} °C"
                )
                Text(
                    text = "Pressure: ${viewModel.weatherState.condition.main.pressure} hPa"
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = viewModel.query,
                        onValueChange = { viewModel.query = it },
                        label = { Text("Query") },
                        modifier = Modifier
                            .weight(0.75f)
                    )
                    Button(
                        onClick = {
                            viewModel.query = ""
                            viewModel.hideErrorMessage()
                            viewModel.getWeatherOfFirstResult()
                        },
                        enabled = viewModel.searchedLocations.isNotEmpty(),
                        modifier = Modifier.weight(0.25f)
                    ) {
                        Text(
                            text = "Find"
                        )
                    }
                }

                LazyColumn {
                    items(viewModel.searchedLocations) { location ->
                        Text(
                            text = location.getFullName(),
                            modifier = Modifier
                                .clickable {
                                    viewModel.query = ""
                                    viewModel.hideErrorMessage()
                                    viewModel.getWeatherOfSelectedLocation(location)
                                }
                                .padding(5.dp)
                                .background(
                                    color = Color.LightGray.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                    }
                }
                if (viewModel.searchedLocations.isEmpty() && viewModel.query.isNotBlank() && viewModel.showNoResults) {
                    Text(
                        text = "No results found"
                    )
                }

                if (viewModel.errorVisible) {
                    Text(
                        color = Color.Red,
                        text = viewModel.errorMessage
                    )
                }
            }
        }
    }
}