package com.example.augmentedrealityglasses.weather.screen

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.augmentedrealityglasses.R
import com.example.augmentedrealityglasses.UpdateWrapper
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
    onTextFieldClick: () -> Unit,
    onScreenComposition: () -> Unit
) {
    //Main UI state
    val uiState by viewModel.weatherState.collectAsStateWithLifecycle()

    //Context
    val context = LocalContext.current

    //Client for fetching the geolocation infos
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient((context))
    }

    LaunchedEffect(Unit) {
        onScreenComposition()
        viewModel.hideErrorMessage()
        if (!viewModel.isDataAvailable()) {
            viewModel.getGeolocationWeather(fusedLocationClient, context)
        }
    }

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


    // ----  UI  ----

    if (!viewModel.isLoading) {
        if (viewModel.isDataAvailable()) {
            UpdateWrapper(
                message = viewModel.errorMessage,
                bluetoothUpdateStatus = viewModel.bluetoothUpdateStatus,
                onErrorDismiss = { viewModel.hideErrorMessage() },
                onBluetoothUpdateDismiss = { viewModel.hideBluetoothUpdate() }
            ) {
                Scaffold(
                    topBar = {
                        LocationBar(
                            uiState.location.getFullName()
                        )
                    }
                ) { innerPadding ->

                    //List state attached to the main Column (that contains all the screen's content)
                    val scrollState = rememberScrollState()

                    //Allow scrolling only when the screen is fully scrolled to the top and the user is not scrolling the LazyRow (Daily forecasts panel)
                    val canRefresh by remember(scrollState, dailyListState) {
                        derivedStateOf {
                            (scrollState.value == 0) && !dailyListState.isScrollInProgress
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
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {

                            currentCondition?.let { condition ->
                                CurrentWeatherBar(
                                    condition.temp,
                                    condition.tempMax,
                                    condition.tempMin,
                                    condition.feelsLike,
                                    condition.main,
                                    condition.iconId
                                )
                            }

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

                            if (dailyForecasts.isNotEmpty()) {
                                DailyForecastsPanel(dailyForecasts, dailyListState)

                                MultipleDaysForecastsPanel(
                                    forecasts = daysConditions,
                                    onItemClick = {
                                        viewModel.changeSelectedDay(it)
                                    },
                                    isSelectedDay = { uiState.selectedDay == it }
                                )
                            }

                            currentCondition?.let { condition ->
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
        } else {
            //data not available
            if (viewModel.errorMessage.isNotEmpty()) {
                val iconId = when (viewModel.errorMessage) {
                    Constants.ERROR_NETWORK_CURRENT_WEATHER, Constants.ERROR_NETWORK_FORECASTS -> {
                        R.drawable.wifi_off
                    }

                    Constants.ERROR_GEOLOCATION_NOT_AVAILABLE -> {
                        R.drawable.location_off
                    }

                    else -> {
                        R.drawable.generic_error
                    }
                }

                WeatherErrorScreen(
                    msg = viewModel.errorMessage,
                    iconId = iconId,
                    onRetry = {
                        viewModel.hideErrorMessage()
                        viewModel.getGeolocationWeather(fusedLocationClient, context)
                    }
                )
            }
        }

    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingAnimation(modifier = Modifier.size(100.dp))
        }

    }
}

@Composable
fun LocationBar(locationName: String) {
    val theme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp, bottom = 15.dp)
            .background(theme.background),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(theme.background)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.location),
                contentDescription = null,
                modifier = Modifier.size(25.dp),
                tint = theme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = locationName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 25.sp,
                ),
                color = theme.primary
            )
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
    val theme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(theme.background),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.background(theme.background)
        ) {
            Text(
                text = "${temperature}°",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 64.sp
                ),
                color = theme.primary
            )
            Text(
                text = conditionName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 24.sp,
                ),
                color = theme.primary
            )
        }

        Column(
            modifier = Modifier.background(theme.background)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.background(theme.background)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_upward),
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(end = 2.dp),
                    tint = theme.secondary
                )
                Text(
                    text = "${maxTemperature}°",
                    style = MaterialTheme.typography.labelLarge,
                    color = theme.secondary
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "/",
                    style = MaterialTheme.typography.labelLarge,
                    color = theme.secondary
                )

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    painter = painterResource(id = R.drawable.arrow_downward),
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(end = 2.dp),
                    tint = theme.secondary
                )
                Text(
                    text = "${minTemperature}°",
                    style = MaterialTheme.typography.labelLarge,
                    color = theme.secondary
                )
            }

            Text(
                text = "Feels Like: ${feelsLike}°",
                style = MaterialTheme.typography.labelLarge,
                color = theme.secondary,
                modifier = Modifier.padding(top = 5.dp),
                maxLines = 1
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
    val theme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = theme.tertiaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(56.dp)
            .clickable { onClickSearchBar() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fake TextField (just UI, no input)
            Icon(
                painter = painterResource(id = R.drawable.search),
                contentDescription = null,
                tint = theme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Search other locations",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.sp
                ),
                color = theme.secondary,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp)
                    .clickable(
                        enabled = !geolocationEnabled
                    ) { onClickGeolocationIcon() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.geolocation),
                    contentDescription = null,
                    tint = if (geolocationEnabled) theme.secondary else theme.primary
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
    val theme = MaterialTheme.colorScheme

    Card(
        shape = RoundedCornerShape(12.dp),
        //elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = theme.onPrimaryContainer),
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Daily forecasts",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp
                ),
                color = theme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                thickness = 1.dp,
                color = Color.LightGray //TODO: adjust color
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
    val theme = MaterialTheme.colorScheme

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 7.dp)
    ) {
        Text(
            text = if (isCurrent) "Now" else timeFmt.format(dateTime),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 14.sp
            ),
            color = theme.primary
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
                fontSize = 23.sp,
            ),
            color = theme.primary
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
    val theme = MaterialTheme.colorScheme

    Card(
        shape = RoundedCornerShape(12.dp),
        //elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = theme.onPrimaryContainer),
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
                    fontSize = 15.sp
                ),
                color = theme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                thickness = 1.dp,
                color = Color.LightGray //TODO: adjust color
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
    val theme = MaterialTheme.colorScheme
    val backgroundColor = if (isSelected) theme.onSurface else theme.onPrimaryContainer
    val contentColor = if (isSelected) theme.inversePrimary else theme.primary

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
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 14.sp,
            ),
            color = contentColor,
            modifier = Modifier.weight(1f)
        )

        Image(
            painter = painterResource(id = iconId),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

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
                Text(
                    text = "${tempMax}°",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 14.sp
                    ),
                    color = contentColor
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "/",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 14.sp
                    ),
                    color = contentColor
                )

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    painter = painterResource(id = R.drawable.arrow_downward),
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(end = 2.dp),
                    tint = contentColor
                )
                Text(
                    text = "${tempMin}°",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 14.sp
                    ),
                    color = contentColor
                )
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
            //TODO: add attribution: https://www.flaticon.com/free-icon/gauge_4284060?related_id=4283902&origin=search
            StatBox(
                title = "Pressure",
                iconRes = R.drawable.pressure,
                content = {
                    StatValue(
                        value = pressure.toString(),
                        unit = "hPa"
                    )
                },
                modifier = Modifier.weight(1f),
                iconSize = 47.dp
            )

            StatBox(
                title = "Humidity",
                iconRes = R.drawable.humidity,
                content = {
                    StatValue(
                        value = humidity.toString(),
                        unit = "%"
                    )
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

            //TODO: add attribution: https://www.flaticon.com/free-icon/wind_2529971?term=wind&page=3&position=43&origin=search&related_id=2529971
            StatBox(
                title = "Wind Speed",
                iconRes = R.drawable.wind,
                content = {
                    StatValue(value = windSpeed.toString(), unit = "km/h")
                },
                modifier = Modifier.weight(1f),
                iconSize = 43.dp
            )
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    iconSize: Dp = 34.dp
) {
    val theme = MaterialTheme.colorScheme

    Card(
        shape = RoundedCornerShape(12.dp),
        //elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = theme.onPrimaryContainer),
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
                style = MaterialTheme.typography.bodyMedium,
                color = theme.primary
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
                        modifier = Modifier.size(iconSize)
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
    val theme = MaterialTheme.colorScheme

    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 26.sp,
            ),
            color = theme.primary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = unit,
            style = MaterialTheme.typography.bodyMedium,
            color = theme.secondary
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
    val theme = MaterialTheme.colorScheme

    Card(
        shape = RoundedCornerShape(12.dp),
        //elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = theme.onPrimaryContainer),
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
                style = MaterialTheme.typography.bodyMedium,
                color = theme.primary
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.sunrise),
                            contentDescription = null,
                            modifier = Modifier.size(iconSize)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = timeFmt.format(sunrise),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 26.sp
                            ),
                            color = theme.primary
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .width(dividerWidth),
                        thickness = 1.dp,
                        color = Color.LightGray //TODO: adjust color
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
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 26.sp
                            ),
                            color = theme.primary
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
        SimpleDateFormat(pattern, Locale.ENGLISH)
    }