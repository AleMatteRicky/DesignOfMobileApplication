package com.example.augmentedrealityglasses.weather.screen

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.augmentedrealityglasses.weather.constants.Constants
import com.example.augmentedrealityglasses.weather.viewmodel.WeatherViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = viewModel()
) {
    //Context
    val context = LocalContext.current

    //Geolocation Permissions
    val hasCoarseLocationPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val hasFineLocationPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val requestPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted) {
            Toast.makeText(context, "Fine location granted", Toast.LENGTH_SHORT).show()
        } else if (coarseLocationGranted) {
            Toast.makeText(context, "Approximate location granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No permissions granted", Toast.LENGTH_SHORT).show()
        }

        //update the state
        hasCoarseLocationPermission.value = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        hasFineLocationPermission.value = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    //Client for fetching the geolocation infos
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient((context))
    }

    //Geolocation state
    val geolocationEnabled = remember { mutableStateOf(false) }

    LaunchedEffect(hasCoarseLocationPermission.value, hasFineLocationPermission.value) {
        if (!hasCoarseLocationPermission.value && !hasFineLocationPermission.value) {
            //request permissions
            requestPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }

        if (hasCoarseLocationPermission.value || hasFineLocationPermission.value) {
            viewModel.fetchCurrentLocation(context, fusedLocationClient, geolocationEnabled)
        }
    }

    //Main UI state (current weather conditions)
    val uiStateCondition by viewModel.uiState.collectAsStateWithLifecycle()

    //Selected location to display the weather conditions for
    val location by remember { derivedStateOf { viewModel.location } }

    //Input for searching the location
    var query by remember { mutableStateOf("") }

    //For managing the visibility of the Text "no results found"
    val showNoResults = remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        showNoResults.value = false
        if (query.isBlank()) {
            viewModel.clearSearchedLocationList()
            return@LaunchedEffect
        }

        delay(Constants.DEBOUNCE_DELAY)
        viewModel.findLocationsByQuery(query, showNoResults)
    }

    // ----  UI  ----
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = {
                if (geolocationEnabled.value) {
                    getGeolocation(
                        context,
                        requestPermissionsLauncher,
                        hasFineLocationPermission,
                        hasCoarseLocationPermission,
                        fusedLocationClient,
                        geolocationEnabled,
                        viewModel
                    )
                }
                viewModel.updateInfos(geolocationEnabledState = geolocationEnabled)
            }) {
                Text(
                    text = "Update weather info"
                )
            }
            Button(
                onClick = {
                    query = ""
                    getGeolocation(
                        context,
                        requestPermissionsLauncher,
                        hasFineLocationPermission,
                        hasCoarseLocationPermission,
                        fusedLocationClient,
                        geolocationEnabled,
                        viewModel
                    )
                },
                enabled = !geolocationEnabled.value
            ) {
                Text(
                    text = "Geolocation weather"
                )
            }
        }
        Text(
            text = if (geolocationEnabled.value) "Current location: ${location.getFullName()}" else "Searched location: ${location.getFullName()}",
            color = if (geolocationEnabled.value) Color.Red else Color.Black
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
        Row(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Query") },
                modifier = Modifier
                    .weight(0.75f)
            )
            Button(
                onClick = {
                    query = ""
                    viewModel.getWeatherOfFirstResult(geolocationEnabled)
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
                            query = ""
                            viewModel.findWeatherInfosByLocation(location, geolocationEnabled)
                            viewModel.clearSearchedLocationList()
                        }
                        .padding(5.dp)
                        .background(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                )
            }
        }
        if (viewModel.searchedLocations.isEmpty() && query.isNotBlank() && showNoResults.value) {
            Text(
                text = "No results found"
            )
        }
    }
}


//Logic functions
fun getGeolocation(
    context: Context,
    requestPermissionsLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>,
    hasFineLocationPermission: MutableState<Boolean>,
    hasCoarseLocationPermission: MutableState<Boolean>,
    fusedLocationClient: FusedLocationProviderClient,
    geolocationEnabled: MutableState<Boolean>,
    viewModel: WeatherViewModel
) {
    when {
        hasCoarseLocationPermission.value || hasFineLocationPermission.value -> {
            //fetch the position
            viewModel.fetchCurrentLocation(context, fusedLocationClient, geolocationEnabled)
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