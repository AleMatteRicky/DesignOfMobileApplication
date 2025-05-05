package com.example.augmentedrealityglasses.weather.constants

object Constants {
    const val WEATHER_API_BASE_URL = "https://api.openweathermap.org/"
    const val DEBOUNCE_DELAY = 500L
    const val ERROR_DISPLAY_TIME = 5000L
    private const val MAX_AGE_LAST_LOCATION_MINUTES = 5
    const val MAX_AGE_LAST_LOCATION = MAX_AGE_LAST_LOCATION_MINUTES * 60 * 1000
    const val MAX_LOCATIONS_FETCHED = "5"

    //TODO: put 40
    const val NUMBER_OF_TIMESTAMPS_FORECAST = "6"
    const val API_UNIT_OF_MEASUREMENT = "metric"
}