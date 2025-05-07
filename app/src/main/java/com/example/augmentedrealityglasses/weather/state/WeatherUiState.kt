package com.example.augmentedrealityglasses.weather.state


data class WeatherUiState(
    val conditions: List<WeatherCondition>,
    val shownTimestamp: String
)

data class WeatherCondition(
    val main: String,
    val description: String,
    val temp: String,
    val pressure: String,
    val timestamp: String,
    val isCurrent: Boolean
)

data class WeatherLocation(
    val name: String,
    val lat: String,
    val lon: String,
    val country: String,
    val state: String?
) {
    fun getFullName(): String {
        return if (this.name.isEmpty()) {
            ""
        } else if (this.state.isNullOrEmpty()) {
            this.name + " (" + this.country + ")"
        } else {
            this.name + ", " + this.state + " (" + this.country + ")"
        }
    }
}