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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.augmentedrealityglasses.weather.constants.Constants
import com.example.augmentedrealityglasses.weather.viewmodel.WeatherViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    onNavigateToHome: () -> Unit
) {
    //Context
    val context = LocalContext.current

    //Client for fetching the geolocation infos
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient((context))
    }

    val requestPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (coarseLocationGranted || fineLocationGranted) {
            viewModel.getGeolocationWeather(fusedLocationClient, context)
        } else {
            //TODO: handle
        }
    }

    var isFirstLaunch by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(isFirstLaunch) {
        if (isFirstLaunch) {
            if (viewModel.getGeolocationPermissions(context).values.none { it }) {
                //request permissions
                requestPermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            } else {
                viewModel.isLoading = true
                viewModel.getGeolocationWeather(fusedLocationClient, context)
            }
            isFirstLaunch = false
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
                Text(text = "Loading data...")
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
                            viewModel.refreshWeatherInfos(fusedLocationClient, context)
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
                            viewModel.getGeolocationWeather(fusedLocationClient, context)
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
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    ForecastDropdown(
                        viewModel
                    )
                    Button(
                        onClick = { viewModel.showCurrentWeather() }
                    ) {
                        Text(
                            text = "Current weather"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Date and time: ${formatDate(viewModel.weatherUI.timestamp)}"
                )
                Text(
                    text = "Info: ${viewModel.weatherUI.main}"
                )
                Text(
                    text = "Description: ${viewModel.weatherUI.description}"
                )
                Text(
                    text = "Temperature: ${viewModel.weatherUI.temp} °C"
                )
                Text(
                    text = "Pressure: ${viewModel.weatherUI.pressure} hPa"
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
            Button(
                onClick = { onNavigateToHome() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Home"
                )
            }
        }
    }
}

@Composable
fun ForecastDropdown(
    viewModel: WeatherViewModel
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Box(modifier = Modifier
            .clickable { expanded = true }
            .background(Color.LightGray.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
            .padding(12.dp)
        ) {
            Text(
                text = "Select date and time of forecast"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            viewModel.weatherState.forecasts.list.forEach { forecast ->
                DropdownMenuItem(
                    text = { Text(formatDate(forecast.dt) ?: "") },
                    onClick = {
                        expanded = false
                        viewModel.showWeatherForecast(forecast.dt)
                    }
                )
            }
        }
    }
}

fun formatDate(timestampSeconds: String): String? {
    if (timestampSeconds != "current" && timestampSeconds != "") {
        try {
            val date = Date((timestampSeconds.toLong()) * 1000)
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            format.timeZone =
                TimeZone.getTimeZone("Europe/Rome") //TODO: get time zone from app settings
            return format.format(date)
        } catch (e: NumberFormatException) {
            //TODO: handle
        }
        return null
    } else {
        return timestampSeconds
    }
}