package com.example.augmentedrealityglasses.weather.constants

object Constants {
    const val WEATHER_API_BASE_URL = "https://api.openweathermap.org/"

    const val DEBOUNCE_DELAY = 500L

    const val ERROR_DISPLAY_TIME = 5000L

    private const val MAX_AGE_LAST_LOCATION_MINUTES = 5
    const val MAX_AGE_LAST_LOCATION = MAX_AGE_LAST_LOCATION_MINUTES * 60 * 1000

    const val MAX_LOCATIONS_FETCHED = "5"

    const val NUMBER_OF_TIMESTAMPS_FORECAST = "40" //max value: 40

    const val API_UNIT_OF_MEASUREMENT = "metric"

    const val ERROR_GENERIC_CURRENT_WEATHER = "Generic error while fetching current weather information. Try again"
    const val ERROR_NETWORK_CURRENT_WEATHER = "Network error while fetching current weather information. Try again"

    const val ERROR_GENERIC_FORECASTS = "Generic error while fetching weather forecasts information. Try again"
    const val ERROR_NETWORK_FORECASTS = "Network error while fetching weather forecasts information. Try again"

    const val ERROR_GENERIC_LOCATIONS = "Generic error while searching locations. Try again"
    const val ERROR_NETWORK_LOCATIONS = "Network error while searching locations. Try again"

    const val ERROR_GEOLOCATION_NOT_AVAILABLE = "Position unavailable! Please try again"
    const val ERROR_GEOLOCATION_NO_PERMISSIONS = "No permissions granted"
    const val ERROR_GEOLOCATION_GENERIC = "An error has occurred while retrieve the geolocation!"
}