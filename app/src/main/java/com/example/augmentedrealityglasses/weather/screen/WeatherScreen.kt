package com.example.augmentedrealityglasses.weather.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.augmentedrealityglasses.ErrorWrapper
import com.example.augmentedrealityglasses.R
import com.example.augmentedrealityglasses.weather.state.DayCondition
import com.example.augmentedrealityglasses.weather.state.WeatherCondition
import com.example.augmentedrealityglasses.weather.viewmodel.WeatherViewModel
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    onTextFieldClick: () -> Unit
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

    /*
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
   */

    LaunchedEffect(Unit) {
        viewModel.hideErrorMessage()
        if (viewModel.location.name == "") {
            val isCachedDataValid = viewModel.tryLoadDataFromCache()

            if (!isCachedDataValid) {
                if (viewModel.getGeolocationPermissions(context).values.none { it }) {
                    //request permissions
                    requestPermissionsLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    )
                } else {
                    viewModel.getGeolocationWeather(fusedLocationClient, context)
                }
            }
        }
    }

//    //To make the error message disappear after time
//    LaunchedEffect(viewModel.errorVisible) {
//        if (viewModel.errorVisible) {
//            delay(Constants.ERROR_DISPLAY_TIME)
//            viewModel.hideErrorMessage()
//        }
//    }

    //Handle auto scroll on left of "Daily forecasts panel" when changing the day
    val dailyListState = rememberLazyListState()
    LaunchedEffect(viewModel.selectedDay) {
        dailyListState.animateScrollToItem(0)
    }

    val currentCondition by remember {
        derivedStateOf { viewModel.getCurrentWeather() }
    }

    val dailyForecasts by remember {
        derivedStateOf { viewModel.getDailyForecastsOfSelectedDay().orEmpty() }
    }

    val daysConditions by remember {
        derivedStateOf { viewModel.getDaysConditions() }
    }

    //TODO: swipe down to refresh data
    // ----  UI  ----
    ErrorWrapper(
        message = viewModel.errorMessage,
        onDismiss = { viewModel.hideErrorMessage() }
    ) {
        Scaffold(
            topBar = {
                //TODO: fix background color when scrolling the page
                LocationAndBLEStatusBar(
                    viewModel.location.getFullName(),
                    viewModel.isExtDeviceConnected
                )

            }
        ) { innerPadding ->

            //List state attached to the LazyColumn (that contains all the screen's content)
            val listState = rememberLazyListState()

            //Allow scrolling only when the screen is fully scrolled to the top
            val scrollDownEnabled by remember {
                derivedStateOf {
                    listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
                }
            }

            SwipeDownRefresh(
                isRefreshing = viewModel.isRefreshing,
                scrollDownEnabled = scrollDownEnabled,
                onRefresh = {
                    viewModel.refreshWeatherInfos(fusedLocationClient, context)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    state = listState
                ) {

                    currentCondition?.let { condition ->
                        item {
                            CurrentWeatherBar(
                                condition.temp,
                                condition.tempMax,
                                condition.tempMin,
                                condition.feelsLike,
                                condition.main,
                                condition.iconId
                            )
                        }
                    }

                    item {
                        LocationManagerBar(
                            onClickSearchBar = { onTextFieldClick() },
                            onClickGeolocationIcon = {
                                viewModel.getGeolocationWeather(
                                    fusedLocationClient,
                                    context
                                )
                            },
                            geolocationEnabled = viewModel.geolocationEnabled
                        )
                    }

                    if (dailyForecasts.isNotEmpty()) {
                        item {
                            DailyForecastsPanel(dailyForecasts, dailyListState)
                        }

                        item {
                            MultipleDaysForecastsPanel(
                                forecasts = daysConditions,
                                onItemClick = {
                                    viewModel.changeSelectedDay(it)
                                },
                                isSelectedDay = { viewModel.selectedDay == it }
                            )
                        }
                    }
                }
            }
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

@Composable
fun LocationManagerBar(
    onClickSearchBar: () -> Unit,
    onClickGeolocationIcon: () -> Unit,
    geolocationEnabled: Boolean
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

            // Fake TextField (just UI, no input)
            Surface(
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.LightGray),
                modifier = Modifier
                    .height(56.dp)
                    .weight(1f)
                    .clickable { onClickSearchBar() },
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = null,
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Search other locations",
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onClickGeolocationIcon,
                enabled = !geolocationEnabled,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Transparent)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.geolocation),
                    contentDescription = null,
                    tint = if (geolocationEnabled) Color.Gray else Color.Black
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
            .padding(bottom = 8.dp)
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


            forecasts.forEach { forecast ->
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