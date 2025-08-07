package com.example.augmentedrealityglasses.weather.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import com.example.augmentedrealityglasses.weather.state.DayCondition
import com.example.augmentedrealityglasses.weather.state.WeatherCondition
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

    //Handle auto scroll on left of "Daily forecasts panel" when changing the day
    val dailyListState = rememberLazyListState()
    LaunchedEffect(viewModel.selectedDay) {
        dailyListState.animateScrollToItem(0)
    }

    // ----  UI  ----
    Column {
        LocationAndBLEStatusBar(
            viewModel.location.getFullName(),
            viewModel.isExtDeviceConnected
        )

        val currentCondition = viewModel.getCurrentWeather()
        if (currentCondition != null) {
            CurrentWeatherBar(
                currentCondition.temp,
                currentCondition.tempMax,
                currentCondition.tempMin,
                currentCondition.feelsLike,
                currentCondition.main,
                currentCondition.iconId
            )
        }

        LocationManagerBar(
            viewModel.query,
            onQueryChange = { viewModel.updateQuery(it) },
            onClickGeolocationIcon = {
                viewModel.getGeolocationWeather(
                    fusedLocationClient,
                    context
                )
            }
        )

        var conditions = viewModel.getAllConditions()
        if (conditions.isNotEmpty()) {
            conditions =
                conditions.sortedBy { it.dateTime }
                    .filter { condition -> condition.dateTime >= viewModel.selectedDay }
                    .take(Constants.DAILY_CONDITIONS_TO_SHOW)

            DailyForecastsPanel(conditions, dailyListState)

            MultipleDaysForecastsPanel(
                forecasts = viewModel.getDaysConditions(),
                onItemClick = { viewModel.changeSelectedDay(it) },
                isSelectedDay = { viewModel.selectedDay == it }
            )
        }
    }
}

@Composable
fun LocationAndBLEStatusBar(locationName: String, isExtDeviceConnected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp),
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
                text = locationName,
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
    temperature: Int,
    maxTemperature: Int,
    minTemperature: Int,
    feelsLike: Int,
    conditionName: String,
    iconId: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "${temperature}°",
                fontSize = 45.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = conditionName,
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
                Text(text = "${maxTemperature}°", fontSize = 14.sp, color = Color.Gray)

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
                Text(text = "${minTemperature}°", fontSize = 14.sp, color = Color.Gray)
            }

            Text(
                text = "Feels Like: ${feelsLike}°",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Image(
            painter = painterResource(id = iconId),
            contentDescription = conditionName,
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
    forecasts: List<WeatherCondition>,
    listState: LazyListState
) {

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
                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState), // Enables snapping to the start of each item during horizontal scroll.

                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(forecasts) { forecast ->
                    DailyForecastItem(
                        forecast.isCurrent,
                        forecast.dateTime,
                        forecast.iconId,
                        forecast.main,
                        forecast.temp
                    )
                }
            }
        }
    }
}

@Composable
fun DailyForecastItem(
    isCurrent: Boolean,
    dateTime: Date,
    iconId: Int,
    conditionName: String,
    temperature: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 7.dp)
    ) {
        Text(
            text = if (isCurrent) "Now" else formatDate(dateTime),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold
            )
        )
        Image(
            painter = painterResource(id = iconId),
            contentDescription = conditionName,
            modifier = Modifier
                .size(30.dp)
        )
        Text(
            text = "${temperature}°",
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

@Composable
fun MultipleDaysForecastsPanel(
    forecasts: List<DayCondition>,
    onItemClick: (Date) -> Unit,
    isSelectedDay: (Date) -> Boolean
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Next days forecasts",
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

            LazyColumn {
                items(forecasts) { forecast ->
                    MultipleDaysForecastsItem(
                        isSelected = isSelectedDay(forecast.date),
                        isCurrentDay = forecast.isCurrent,
                        date = forecast.date,
                        iconId = forecast.iconId,
                        tempMax = forecast.tempMax,
                        tempMin = forecast.tempMin,
                        onClick = { onItemClick(forecast.date) }
                    )
                }
            }
        }
    }
}

@Composable
fun MultipleDaysForecastsItem(
    isSelected: Boolean,
    isCurrentDay: Boolean,
    date: Date,
    iconId: Int,
    tempMax: Int,
    tempMin: Int,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color.Black else Color.Transparent
    val contentColor = if (isSelected) Color.White else Color.Black

    //Disable item's click when already selected
    val clickableModifier = if (isSelected) {
        Modifier //not clickable
    } else {
        Modifier.clickable(onClick = onClick) //clickable
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .background(backgroundColor, shape = RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isCurrentDay) "Today" else getDayName(date),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = contentColor
            ),
            modifier = Modifier.weight(1f)
        )

        Image(
            painter = painterResource(id = iconId),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        //FIXME: handle spacing when there are no min/max temperatures
        if (!isCurrentDay) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_upward),
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(end = 2.dp),
                    tint = contentColor
                )
                Text(text = "${tempMax}°", fontSize = 14.sp, color = contentColor)

                Spacer(modifier = Modifier.width(4.dp))

                Text(text = "/", color = contentColor)

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    painter = painterResource(id = R.drawable.arrow_downward),
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(end = 2.dp),
                    tint = contentColor
                )
                Text(text = "${tempMin}°", fontSize = 14.sp, color = contentColor)
            }
        }
    }
}

fun getDayName(date: Date): String {
    //TODO: language setting
    val format = SimpleDateFormat("EEEE", Locale.ENGLISH) //It provides the complete name of the day
    return format.format(date)
}