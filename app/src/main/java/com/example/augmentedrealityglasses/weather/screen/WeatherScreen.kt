package com.example.augmentedrealityglasses.weather.screen

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.augmentedrealityglasses.ErrorWrapper
import com.example.augmentedrealityglasses.R
import com.example.augmentedrealityglasses.translation.ui.LoadingAnimation
import com.example.augmentedrealityglasses.weather.constants.Constants
import com.example.augmentedrealityglasses.weather.state.DayCondition
import com.example.augmentedrealityglasses.weather.state.WeatherCondition
import com.example.augmentedrealityglasses.weather.state.getDailyIconForConditions
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
    //Main UI state
    val uiState by viewModel.weatherState.collectAsStateWithLifecycle()

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
        if (uiState.location.name == "") {
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

//    //To make the error message disappear after time
//    LaunchedEffect(viewModel.errorVisible) {
//        if (viewModel.errorVisible) {
//            delay(Constants.ERROR_DISPLAY_TIME)
//            viewModel.hideErrorMessage()
//        }
//    }

    val currentCondition by remember(uiState.conditions) {
        derivedStateOf { uiState.conditions.firstOrNull { it.isCurrent } }
    }

    val dailyForecasts by remember(uiState.selectedDay, uiState.conditions) {
        derivedStateOf {
            uiState.conditions.sortedBy { it.dateTime }
                .filter { condition -> condition.dateTime >= uiState.selectedDay }
                .take(Constants.DAILY_CONDITIONS_TO_SHOW)
        }
    }

    val daysConditions by remember(uiState.conditions) {
        derivedStateOf {
            uiState.conditions.groupBy { condition -> viewModel.startOfDay(condition.dateTime) }
                .map { (date, conditions) ->
                    DayCondition(
                        date = date,
                        isCurrent = conditions.any { condition ->
                            condition.isCurrent
                        },
                        iconId = getDailyIconForConditions(conditions),
                        tempMin = conditions.minOf { it.tempMin },
                        tempMax = conditions.maxOf { it.tempMax }
                    )
                }
                .sortedBy { it.date }
        }
    }

    //Handle auto scroll on left of "Daily forecasts panel" when changing the day
    val dailyListState = rememberLazyListState()
    LaunchedEffect(uiState.selectedDay, dailyForecasts) {
        if (dailyForecasts.isNotEmpty()) {
            dailyListState.animateScrollToItem(0)
        }
    }

    //TODO: swipe down to refresh data
    // ----  UI  ----

    if (!viewModel.isLoading) {
        ErrorWrapper(
            message = viewModel.errorMessage,
            onDismiss = { viewModel.hideErrorMessage() }
        ) {
            Scaffold(
                topBar = {
                    //TODO: fix background color when scrolling the page
                    LocationAndBLEStatusBar(
                        uiState.location.getFullName(),
                        viewModel.isExtDeviceConnected
                    )

                }
            ) { innerPadding ->

                //List state attached to the LazyColumn (that contains all the screen's content)
                val listState = rememberLazyListState()

                //Allow scrolling only when the screen is fully scrolled to the top and the user is not scrolling the LazyRow (Daily forecasts panel)
                val canRefresh by remember(listState, dailyListState) {
                    derivedStateOf {
                        Log.d("ciao", dailyListState.isScrollInProgress.toString())
                        !(listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) && !dailyListState.isScrollInProgress
                    }
                }

                SwipeDownRefresh(
                    isRefreshing = viewModel.isRefreshing,
                    canRefresh = canRefresh,
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
                                geolocationEnabled = uiState.geolocationEnabled
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
                                    isSelectedDay = { uiState.selectedDay == it }
                                )
                            }
                        }

                        currentCondition?.let { condition ->
                            item {
                                //TODO: make this grid reactive to day changes
                                AdditionalInfosGrid(
                                    pressure = condition.pressure,
                                    humidity = condition.humidity,

                                    //Current condition must have these params
                                    sunrise = condition.sunrise!!,
                                    sunset = condition.sunset!!,

                                    windSpeed = condition.windSpeed
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingAnimation(modifier = Modifier.size(100.dp))
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
    val timeFmt = rememberTimeFormatter("HH:mm")

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 7.dp)
    ) {
        Text(
            text = if (isCurrent) "Now" else timeFmt.format(dateTime),
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

@Composable
fun rememberTimeFormatter(pattern: String) =
    remember(pattern) {
        SimpleDateFormat(pattern, Locale.getDefault())
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

    val dayNameFmt = rememberDayNameFormatter("EEEE") //It provides the complete name of the day

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .background(backgroundColor, shape = RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isCurrentDay) "Today" else dayNameFmt.format(date),
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

@Composable
fun AdditionalInfosGrid(
    pressure: Int,
    humidity: Int,
    sunrise: Date,
    sunset: Date,
    windSpeed: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatBox(
                title = "Pressure",
                iconRes = R.drawable.pressure,
                content = {
                    StatValue(value = pressure.toString(), unit = "hPa")
                },
                modifier = Modifier.weight(1f)
            )

            StatBox(
                title = "Humidity",
                iconRes = R.drawable.humidity,
                content = {
                    StatValue(value = humidity.toString(), unit = "%")
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SunriseSunsetBox(
                sunrise = sunrise,
                sunset = sunset,
                modifier = Modifier.weight(1f)
            )

            StatBox(
                title = "Wind Speed",
                iconRes = R.drawable.wind,
                content = {
                    StatValue(value = windSpeed.toString(), unit = "km/h")
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (iconRes != null) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            content()
        }
    }
}

@Composable
private fun StatValue(
    value: String,
    unit: String
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = Color.Black
            )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.Gray
            )
        )
    }
}

@Composable
private fun SunriseSunsetBox(
    sunrise: Date,
    sunset: Date,
    modifier: Modifier = Modifier,
    title: String = "Sunrise / Sunset",
    iconSize: Dp = 32.dp,
    dividerWidth: Dp = 64.dp
) {
    val timeFmt = rememberTimeFormatter("HH:mm")

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.sunrise),
                            contentDescription = null,
                            modifier = Modifier.size(iconSize)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = timeFmt.format(sunrise),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .width(dividerWidth),
                        thickness = 1.dp,
                        color = Color.LightGray
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.sunset),
                            contentDescription = null,
                            modifier = Modifier.size(iconSize)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = timeFmt.format(sunset),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun rememberDayNameFormatter(pattern: String) =
    remember(pattern) {
        SimpleDateFormat(pattern, Locale.ENGLISH) //TODO: handle language from settings
    }