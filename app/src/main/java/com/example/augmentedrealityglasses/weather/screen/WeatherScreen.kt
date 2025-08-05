package com.example.augmentedrealityglasses.weather.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.augmentedrealityglasses.R
import com.example.augmentedrealityglasses.weather.constants.Constants
import com.example.augmentedrealityglasses.weather.state.WeatherCondition
import com.example.augmentedrealityglasses.weather.state.WeatherLocation
import com.example.augmentedrealityglasses.weather.viewmodel.WeatherViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel
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
    Column {
        LocationAndBLEStatusBar(viewModel.location, viewModel.isExtDeviceConnected)

        val currentCondition = viewModel.getCurrentWeather()
        if (currentCondition != null) {
            CurrentWeatherBar(currentCondition)
        }

        LocationManagerBar(
            viewModel.query,
            onQueryChange = { viewModel.updateQuery(it) },
            onClickGeolocationIcon = {
                viewModel.getGeolocationWeather(
                    fusedLocationClient,
                    context
                )
            })

        var conditions = viewModel.getAllConditions()
        if (conditions.isNotEmpty()) {
            conditions =
                conditions.sortedBy { it.dateTime }.take(Constants.DAILY_CONDITIONS_TO_SHOW)

            DailyForecastsPanel(conditions)
        }
    }
}

@Composable
fun LocationAndBLEStatusBar(location: WeatherLocation, isExtDeviceConnected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.location),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                //FIXME: handle long texts
                text = location.getFullName(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            )
        }

        Row(
            modifier = Modifier
                .background(Color(0xFFEFEFEF), shape = RoundedCornerShape(16.dp)) //FIXME: fix color
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.eyeglasses),
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            if (isExtDeviceConnected) {
                Icon(
                    painter = painterResource(id = R.drawable.bluetooth_connected),
                    contentDescription = "Device connected",
                    modifier = Modifier.size(30.dp)
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.bluetooth_disabled),
                    contentDescription = "Device not connected",
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
fun CurrentWeatherBar(
    condition: WeatherCondition
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "${condition.temp}°",
                fontSize = 45.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = condition.main,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_upward),
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(end = 2.dp),
                    tint = Color.Gray
                )
                Text(text = "${condition.tempMax}°", fontSize = 14.sp, color = Color.Gray)

                Spacer(modifier = Modifier.width(4.dp))

                Text(text = "/", color = Color.Gray)

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    painter = painterResource(id = R.drawable.arrow_downward),
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(end = 2.dp),
                    tint = Color.Gray
                )
                Text(text = "${condition.tempMin}°", fontSize = 14.sp, color = Color.Gray)
            }

            Text(
                text = "Feels Like: ${condition.feelsLike}°",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Image(
            painter = painterResource(id = condition.iconId),
            contentDescription = condition.main,
            modifier = Modifier
                .size(80.dp)
        )
    }
}

//TODO: complete
@Composable
fun LocationManagerBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClickGeolocationIcon: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            //FIXME: limit Text field width
            OutlinedTextField(
                value = query,
                onValueChange = {
                    onQueryChange(it)
                },
                placeholder = { Text("Search other locations") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = null,
                    )
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(56.dp)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onClickGeolocationIcon,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Transparent)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.geolocation),
                    contentDescription = null
                )
            }

        }
    }
}

//TODO: Add gradient to the right side
@Composable
fun DailyForecastsPanel(
    forecasts: List<WeatherCondition>
) {
    // Enables snapping to the start of each item during horizontal scroll.
    val listState = rememberLazyListState()

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Daily forecasts (updated every 3 hours)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                thickness = 1.dp,
                color = Color.LightGray
            )
            LazyRow(
                // Enables snapping to the start of each item during horizontal scroll.
                state = listState,
                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),

                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(forecasts) { forecast ->
                    DailyForecastItem(forecast)
                }
            }
        }
    }
}

@Composable
fun DailyForecastItem(
    condition: WeatherCondition
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 7.dp)
    ) {
        Text(
            text = if (condition.isCurrent) "Now" else formatDate(condition.dateTime),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold
            )
        )
        Image(
            painter = painterResource(id = condition.iconId),
            contentDescription = condition.main,
            modifier = Modifier
                .size(30.dp)
        )
        Text(
            text = "${condition.temp}°",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
        )
    }
}

fun formatDate(date: Date): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
}