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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.example.augmentedrealityglasses.weather.state.WeatherCondition
import com.example.augmentedrealityglasses.weather.viewmodel.WeatherViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = viewModel.query,
                        onValueChange = { viewModel.query = it },
                        label = { Text("Location") },
                        modifier = Modifier
                            .weight(0.75f),
                        singleLine = true
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
                //FIXME: fix the UI when displaying locations found by the api (and also the "no result" Text)
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
                //FIXME: wrong place
                if (viewModel.errorVisible) {
                    Text(
                        color = Color.Red,
                        text = viewModel.errorMessage
                    )
                }

                Text(
                    text = if (viewModel.geolocationEnabled) "Current location: ${viewModel.location.getFullName()}" else "Searched location: ${viewModel.location.getFullName()}",
                    color = if (viewModel.geolocationEnabled) Color.Red else Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))

                //Current weather info
                val current = viewModel.getCurrentWeather()

                if (current != null) {
                    Condition(current)
                }

                Spacer(modifier = Modifier.height(8.dp))

                //Forecast weather
                Forecasts(
                    conditions = viewModel.getForecasts(),
                    dayShown = viewModel.dayShown,
                    onDayChange = { viewModel.changeDay(it) }
                )
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

//Composable that displays a specific weather condition
@Composable
fun Condition(
    condition: WeatherCondition
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault())

            Text(
                text = "Date and time: ${dateFormat.format(condition.dateTime)}"
            )
            Text(
                text = "Info: ${condition.main}"
            )
            Text(
                text = "Description: ${condition.description}"
            )
            Text(
                text = "Temperature: ${condition.temp} ${Constants.TEMPERATURE_UNIT}"
            )
            Text(
                text = "Pressure: ${condition.pressure} ${Constants.PRESSURE_UNIT}"
            )
        }
    }
}

//Composable that shows forecasts
@Composable
fun Forecasts(
    conditions: List<WeatherCondition>,
    dayShown: Date,
    onDayChange: (Date) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    //This function normalizes the given Date by setting its time to midnight (00:00:00.000), effectively removing the time portion and leaving only the date (year, month, day)
    fun normalizeDate(date: Date): Date {
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.time
    }

    //It contains all available days related to weather conditions
    val availableDays: List<Date> = conditions
        .map { normalizeDate(it.dateTime) }
        .distinct()
        .sorted()

    val normalizedDayShown = normalizeDate(dayShown)

    val currentIndex = availableDays.indexOfFirst { it == normalizedDayShown }

    val filteredConditions = conditions.filter {
        normalizeDate(it.dateTime) == normalizedDayShown
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    if (currentIndex > 0) {
                        onDayChange(availableDays[currentIndex - 1])
                    }
                },
                enabled = currentIndex > 0
            ) {
                Text("<")
            }

            Text(
                text = dateFormat.format(normalizedDayShown),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMedium
            )

            Button(
                onClick = {
                    if (currentIndex < availableDays.lastIndex) {
                        onDayChange(availableDays[currentIndex + 1])
                    }
                },
                enabled = currentIndex < availableDays.lastIndex
            ) {
                Text(">")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredConditions.isEmpty()) {
            Text("No conditions for the day")
        } else {
            //TODO: auto scroll up this when changing day
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredConditions) { condition ->
                    Condition(condition = condition)
                }
            }
        }
    }
}