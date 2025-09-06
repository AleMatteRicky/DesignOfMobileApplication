package com.example.augmentedrealityglasses.weather.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.augmentedrealityglasses.R
import com.example.augmentedrealityglasses.weather.constants.Constants
import com.example.augmentedrealityglasses.weather.viewmodel.WeatherViewModel
import kotlinx.coroutines.delay

@Composable
fun SearchLocationsScreen(
    viewModel: WeatherViewModel,
    onBackClick: () -> Unit
) {
    //It allows to automatically focus on the text field when the screen is loaded
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    var previousQuery by remember { mutableStateOf(viewModel.query) }

    LaunchedEffect(Unit) {
        viewModel.updateQuery("")
    }

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

    val theme = MaterialTheme.colorScheme

    // ----  UI  ----
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = {
                    viewModel.updateQuery(it)
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp,
                    color = theme.primary
                ),
                placeholder = {
                    Text(
                        text = "Search other locations",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 16.sp
                        ),
                        color = theme.secondary
                    )
                },
                singleLine = true,
                leadingIcon = {
                    IconButton(
                        onClick = {
                            onBackClick()
                        },
                        modifier = Modifier.size(31.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_back),
                            contentDescription = "Back",
                            modifier = Modifier.fillMaxSize(),
                            tint = theme.primary
                        )
                    }
                },
                trailingIcon = {
                    if (viewModel.query.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.updateQuery("")
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.cancel),
                                contentDescription = "Clear",
                                modifier = Modifier.fillMaxSize(),
                                tint = theme.primary
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(56.dp)
                    .fillMaxWidth()
                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)) //TODO: adjust color?
                    .widthIn(max = 280.dp) //Limit the text field width
                    .focusRequester(focusRequester) //Link the focus requester
            )
        }

        Spacer(modifier = Modifier.height(15.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (viewModel.showNoResults && viewModel.searchedLocations.isEmpty()) {
                Text(
                    text = "No results found",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 16.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = theme.secondary,
                )
            } else {
                viewModel.searchedLocations.forEachIndexed { index, location ->
                    LocationItem(
                        locationName = location.getFullName(),
                        onClick = {
                            viewModel.getWeatherOfSelectedLocation(location)
                            onBackClick()
                        }
                    )

                    // Divider between items but not after the last element
                    if (index < viewModel.searchedLocations.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 0.6.dp,
                            color = Color.LightGray //TODO: adjust color?
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LocationItem(
    locationName: String,
    onClick: () -> Unit
) {
    val theme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onClick() })
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            tint = theme.secondary
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = locationName,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
            color = theme.primary
        )
    }
}