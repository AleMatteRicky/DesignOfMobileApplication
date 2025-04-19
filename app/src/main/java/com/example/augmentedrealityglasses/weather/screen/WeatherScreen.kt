package com.example.augmentedrealityglasses.weather.screen

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.augmentedrealityglasses.weather.viewmodel.WeatherViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices


@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = viewModel()
) {

    val context = LocalContext.current

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

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient((context))
    }

    val geolocation = viewModel.geolocation.value

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
            viewModel.fetchCurrentLocation(context, fusedLocationClient)
        }
    }

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
            Button(onClick = {
                getGeolocation(
                    context,
                    requestPermissionsLauncher,
                    hasFineLocationPermission,
                    hasCoarseLocationPermission,
                    fusedLocationClient,
                    viewModel
                )
            }) {
                Text(
                    text = "Geolocation weather"
                )
            }
        }
        Text(
            text = "Geolocation: lat ${geolocation.lat} / lon ${geolocation.lon}"
        )
        Text(
            text = "Permission fine: ${hasFineLocationPermission.value}"
        )
        Text(
            text = "Permission coarse: ${hasCoarseLocationPermission.value}"
        )
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

fun getGeolocation(
    context: Context,
    requestPermissionsLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>,
    hasFineLocationPermission: MutableState<Boolean>,
    hasCoarseLocationPermission: MutableState<Boolean>,
    fusedLocationClient: FusedLocationProviderClient,
    viewModel: WeatherViewModel
) {
    when {
        hasCoarseLocationPermission.value || hasFineLocationPermission.value -> {
            //fetch the position
            viewModel.fetchCurrentLocation(context, fusedLocationClient)
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

fun updateWeatherInfo(viewModel: WeatherViewModel) {
    viewModel.updateInfos()
}

fun findWeatherByQuery(query: String, viewModel: WeatherViewModel) {
    if (query.isNotEmpty() && query.isNotBlank()) {
        viewModel.findByQuery(query)
    }
}