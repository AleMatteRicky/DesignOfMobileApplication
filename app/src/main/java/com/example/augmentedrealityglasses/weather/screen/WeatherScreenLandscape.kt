package com.example.augmentedrealityglasses.weather.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.augmentedrealityglasses.R
import com.example.augmentedrealityglasses.update.UpdateWrapper
import com.example.augmentedrealityglasses.translation.ui.LoadingAnimation
import com.example.augmentedrealityglasses.weather.constants.Constants
import com.example.augmentedrealityglasses.weather.state.DayCondition
import com.example.augmentedrealityglasses.weather.state.getDailyIconForConditions
import com.example.augmentedrealityglasses.weather.viewmodel.WeatherViewModel
import com.google.android.gms.location.LocationServices

@Composable
fun WeatherScreenLandscape(
    viewModel: WeatherViewModel,
    onTextFieldClick: () -> Unit,
    onScreenComposition: () -> Unit
) {

    val uiState by viewModel.weatherState.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
                .filter { it.dateTime >= uiState.selectedDay }
                .take(Constants.DAILY_CONDITIONS_TO_SHOW)
        }
    }

    val daysConditions by remember(uiState.conditions) {
        derivedStateOf {
            uiState.conditions.groupBy { c -> viewModel.startOfDay(c.dateTime) }
                .map { (date, conditions) ->
                    DayCondition(
                        date = date,
                        isCurrent = conditions.any { it.isCurrent },
                        iconId = getDailyIconForConditions(conditions),
                        tempMin = conditions.minOf { it.tempMin },
                        tempMax = conditions.maxOf { it.tempMax }
                    )
                }
                .sortedBy { it.date }
        }
    }

    val dailyListState = rememberLazyListState()
    LaunchedEffect(uiState.selectedDay, dailyForecasts) {
        if (dailyForecasts.isNotEmpty()) dailyListState.animateScrollToItem(0)
    }

    if (!viewModel.isLoading) {
        if (viewModel.isDataAvailable()) {
            UpdateWrapper(
                message = viewModel.errorMessage,
                bluetoothUpdateStatus = viewModel.bluetoothUpdateStatus,
                onErrorDismiss = { viewModel.hideErrorMessage() },
                onBluetoothUpdateDismiss = { viewModel.hideBluetoothUpdate() }
            ) {
                Scaffold(
                    topBar = { LocationBar(uiState.location.getFullName()) }
                ) { innerPadding ->

                    val leftScrollState = rememberScrollState()
                    val rightScrollState = rememberScrollState()

                    val canRefresh by remember(leftScrollState, rightScrollState, dailyListState) {
                        derivedStateOf {
                            leftScrollState.value == 0 &&
                                    rightScrollState.value == 0 &&
                                    !dailyListState.isScrollInProgress
                        }
                    }

                    SwipeDownRefresh(
                        isRefreshing = viewModel.isRefreshing,
                        canRefresh = canRefresh,
                        onRefresh = { viewModel.refreshWeatherInfos(fusedLocationClient, context) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {

                            Column(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .fillMaxHeight()
                                    .verticalScroll(leftScrollState)
                            ) {
                                currentCondition?.let { condition ->
                                    CurrentWeatherBarLandscape(
                                        condition.temp,
                                        condition.tempMax,
                                        condition.tempMin,
                                        condition.feelsLike,
                                        condition.main,
                                        condition.iconId
                                    )
                                }

                                Spacer(Modifier.height(24.dp))

                                LocationManagerBar(
                                    onClickSearchBar = onTextFieldClick,
                                    onClickGeolocationIcon = {
                                        viewModel.getGeolocationWeather(
                                            fusedLocationClient,
                                            context
                                        )
                                    },
                                    geolocationEnabled = uiState.geolocationEnabled,
                                    padding = 0.dp
                                )

                                Spacer(Modifier.height(16.dp))

                                currentCondition?.let { condition ->
                                    AdditionalInfosList(
                                        pressure = condition.pressure,
                                        humidity = condition.humidity,
                                        sunrise = condition.sunrise!!,
                                        sunset = condition.sunset!!,
                                        windSpeed = condition.windSpeed
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1.8f)
                                    .fillMaxHeight()
                                    .verticalScroll(rightScrollState)
                            ) {
                                if (dailyForecasts.isNotEmpty()) {
                                    DailyForecastsPanel(
                                        forecasts = dailyForecasts,
                                        listState = dailyListState
                                    )

                                    Spacer(Modifier.height(16.dp))

                                    MultipleDaysForecastsPanel(
                                        forecasts = daysConditions,
                                        onItemClick = { viewModel.changeSelectedDay(it) },
                                        isSelectedDay = { uiState.selectedDay == it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val iconId = when (viewModel.errorMessage) {
                Constants.ERROR_NETWORK_CURRENT_WEATHER, Constants.ERROR_NETWORK_FORECASTS -> R.drawable.wifi_off
                Constants.ERROR_GEOLOCATION_NOT_AVAILABLE -> R.drawable.location_off
                else -> R.drawable.generic_error
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
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingAnimation(modifier = Modifier.size(200.dp))
        }
    }
}

@Composable
fun AdditionalInfosList(
    pressure: Int,
    humidity: Int,
    sunrise: java.util.Date,
    sunset: java.util.Date,
    windSpeed: Float
) {
    val theme = MaterialTheme.colorScheme
    val timeFmt = rememberTimeFormatter("HH:mm")

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = theme.tertiaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = "Details",
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
                color = Color.LightGray
            )

            InfoRow(
                iconRes = R.drawable.pressure,
                title = "Pressure",
                valueContent = {
                    ValueWithUnit(
                        value = pressure.toString(),
                        unit = "hPa"
                    )
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                thickness = 1.dp,
                color = Color.LightGray
            )

            InfoRow(
                iconRes = R.drawable.humidity,
                title = "Humidity",
                valueContent = {
                    ValueWithUnit(
                        value = humidity.toString(),
                        unit = "%"
                    )
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                thickness = 1.dp,
                color = Color.LightGray
            )

            InfoRow(
                iconRes = R.drawable.sunrise,
                title = "Sunrise",
                valueContent = {
                    Text(
                        text = timeFmt.format(sunrise),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.sp
                        ),
                        color = theme.primary
                    )
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                thickness = 1.dp,
                color = Color.LightGray
            )

            InfoRow(
                iconRes = R.drawable.sunset,
                title = "Sunset",
                valueContent = {
                    Text(
                        text = timeFmt.format(sunset),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.sp
                        ),
                        color = theme.primary
                    )
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                thickness = 1.dp,
                color = Color.LightGray
            )

            InfoRow(
                iconRes = R.drawable.wind,
                title = "Wind Speed",
                valueContent = {
                    ValueWithUnit(
                        value = windSpeed.toInt().toString(),
                        unit = "km/h"
                    )
                }
            )
        }
    }
}

@Composable
private fun InfoRow(
    iconRes: Int,
    title: String,
    valueContent: @Composable () -> Unit
) {
    val theme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {

            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(26.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 14.sp
                ),
                color = theme.primary
            )
        }

        Box(contentAlignment = Alignment.CenterEnd) {
            valueContent()
        }
    }
}

@Composable
private fun ValueWithUnit(
    value: String,
    unit: String
) {
    val theme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 20.sp
            ),
            color = theme.primary
        )

        Spacer(Modifier.width(6.dp))

        Text(
            text = unit,
            style = MaterialTheme.typography.bodyMedium,
            color = theme.secondary
        )
    }
}

@Composable
fun CurrentWeatherBarLandscape(
    temperature: Int,
    maxTemperature: Int,
    minTemperature: Int,
    feelsLike: Int,
    conditionName: String,
    iconId: Int,
    padding: Dp = 0.dp,
    secondaryTextSize: TextUnit = 16.sp,
    secondaryIconSize: Dp = 16.dp,
    weatherIconSize: Dp = 80.dp
) {
    val theme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = padding)
            .background(theme.background),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "${temperature}°",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 64.sp),
                        color = theme.primary
                    )

                    Spacer(Modifier.height(1.dp))

                    Text(
                        text = conditionName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 24.sp),
                        color = theme.primary
                    )
                }

                Spacer(Modifier.weight(1f))

                Image(
                    painter = painterResource(id = iconId),
                    contentDescription = conditionName,
                    modifier = Modifier.size(weatherIconSize)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_upward),
                        contentDescription = null,
                        modifier = Modifier
                            .size(secondaryIconSize)
                            .padding(end = 2.dp),
                        tint = theme.secondary
                    )
                    Text(
                        text = "${maxTemperature}°",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = secondaryTextSize),
                        color = theme.secondary
                    )

                    Spacer(modifier = Modifier.width(2.dp))

                    Text(
                        text = "/",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = secondaryTextSize),
                        color = theme.secondary
                    )

                    Spacer(modifier = Modifier.width(2.dp))

                    Icon(
                        painter = painterResource(id = R.drawable.arrow_downward),
                        contentDescription = null,
                        modifier = Modifier
                            .size(secondaryIconSize)
                            .padding(end = 2.dp),
                        tint = theme.secondary
                    )
                    Text(
                        text = "${minTemperature}°",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = secondaryTextSize),
                        color = theme.secondary
                    )
                }

                Text(
                    text = "Feels Like: ${feelsLike}°",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = secondaryTextSize),
                    color = theme.secondary,
                    maxLines = 1
                )
            }
        }
    }
}