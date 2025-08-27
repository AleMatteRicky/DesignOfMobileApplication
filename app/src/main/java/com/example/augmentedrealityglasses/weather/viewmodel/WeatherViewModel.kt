package com.example.augmentedrealityglasses.weather.viewmodel

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.augmentedrealityglasses.App
import com.example.augmentedrealityglasses.ble.devicedata.RemoteDeviceManager
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.ConnectionState
import com.example.augmentedrealityglasses.cache.Cache
import com.example.augmentedrealityglasses.cache.CachePolicy
import com.example.augmentedrealityglasses.cache.DefaultTimeProvider
import com.example.augmentedrealityglasses.weather.constants.Constants
import com.example.augmentedrealityglasses.weather.network.APIResult
import com.example.augmentedrealityglasses.weather.network.APIWeatherCondition
import com.example.augmentedrealityglasses.weather.network.APIWeatherForecasts
import com.example.augmentedrealityglasses.weather.network.WeatherRepositoryImpl
import com.example.augmentedrealityglasses.weather.state.GeolocationResult
import com.example.augmentedrealityglasses.weather.state.WeatherCondition
import com.example.augmentedrealityglasses.weather.state.WeatherLocation
import com.example.augmentedrealityglasses.weather.state.WeatherSnapshot
import com.example.augmentedrealityglasses.weather.state.WeatherUiState
import com.example.augmentedrealityglasses.weather.state.createWeatherSnapshot
import com.example.augmentedrealityglasses.weather.state.toModel
import com.example.augmentedrealityglasses.weather.state.toModelList
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

class WeatherViewModel(
    private val repository: WeatherRepositoryImpl,
    private val proxy: RemoteDeviceManager,
    private val cache: Cache,
    private val cachePolicy: CachePolicy
) : ViewModel() {

    //Initialize the viewModel
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val weatherAPIRepository =
                    (this[APPLICATION_KEY] as App).container.weatherAPIRepository
                val bleManager =
                    (this[APPLICATION_KEY] as App).container.proxy
                val cache =
                    (this[APPLICATION_KEY] as App).container.weatherCache
                val cachePolicy =
                    (this[APPLICATION_KEY] as App).container.weatherCachePolicy
                WeatherViewModel(
                    repository = weatherAPIRepository,
                    proxy = bleManager,
                    cache = cache,
                    cachePolicy = cachePolicy
                )
            }
        }
    }

    // Tracks the Bluetooth connection status with the external device
    var isExtDeviceConnected by mutableStateOf(false)
        private set

    // Start listening for Bluetooth packets
    init {
        viewModelScope.launch {
            if (proxy.isDeviceSet()) {
                proxy.receiveUpdates()
                    .collect { connectionState ->
                        isExtDeviceConnected =
                            connectionState.connectionState is ConnectionState.Connected
                    }
            }
        }
    }

    //Tag for logging
    private val TAG = "weather_viewModel"

    //Main UI state
    private val _weatherState = MutableStateFlow(
        WeatherUiState(
            conditions = listOf(),
            selectedDay = Date(),
            geolocationEnabled = true,
            location = WeatherLocation(
                "",
                "",
                "",
                "",
                ""
            )
        )
    )
    val weatherState: StateFlow<WeatherUiState> = _weatherState

    //Flag for the loading animation
    var isLoading by mutableStateOf(false)

    //List of all the locations found by the API (by specifying a query)
    private var _searchedLocations = mutableStateListOf<WeatherLocation>()
    val searchedLocations: List<WeatherLocation> get() = _searchedLocations

    //For managing the visibility of the Text "no results found"
    var showNoResults by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf("")
        private set

    //Input for searching the location
    var query by mutableStateOf("")
        private set

    //Handle the swipe down gesture
    var isRefreshing by mutableStateOf(false)
        private set

    // LOGIC FUNCTIONS

    /**
     * It allows to update weatherState. Put null parameters for non changing fields
     */
    private fun applyState(
        newLocation: WeatherLocation? = null,
        newConditions: List<WeatherCondition>? = null,
        newGeolocationFlag: Boolean? = null,
        newSelectedDay: Date? = null
    ) {
        _weatherState.update { old ->
            val loc = (newLocation ?: old.location).let {
                it.copy(state = it.state ?: "")
            }

            val conds = newConditions ?: old.conditions
            val selectedDay = newSelectedDay ?: old.selectedDay
            val geo = newGeolocationFlag ?: old.geolocationEnabled

            old.copy(
                conditions = conds,
                location = loc,
                geolocationEnabled = geo,
                selectedDay = selectedDay
            )
        }
    }

    //Cache functions
    private fun saveWeatherSnapshotIntoCache() {
        val snapshot = createWeatherSnapshot(
            location = weatherState.value.location,
            conditions = weatherState.value.conditions
        )

        viewModelScope.launch(Dispatchers.IO) {
            cache.set(
                key = Constants.WEATHER_CACHE_KEY,
                value = snapshot,
                serializer = WeatherSnapshot.serializer(),
                timeProvider = DefaultTimeProvider
            )
        }
    }

    private suspend fun tryLoadDataFromCache(): Boolean {

        val snap = withContext(Dispatchers.IO) {
            cache.getIfValid(
                key = Constants.WEATHER_CACHE_KEY,
                policy = cachePolicy,
                serializer = WeatherSnapshot.serializer(),
                timeProvider = DefaultTimeProvider
            )
        } ?: return false

        val loc = withContext(Dispatchers.IO) { snap.location.toModel() }
        val conds = withContext(Dispatchers.IO) { snap.conditions.toModelList() }
        val selectedDay = startOfDay(getMinDateOfAvailableConditions(conds))

        applyState(
            newConditions = conds,
            newLocation = loc,
            newGeolocationFlag = true,
            newSelectedDay = selectedDay
        )
        return true
    }

    fun updateQuery(newQuery: String) {
        query = newQuery
    }

    fun clearSearchedLocationList() {
        _searchedLocations.clear()
    }

    /**
     * This method sends the update message to the external device. The message is created with the current weatherState value (location and conditions are read)
     */
    private fun sendBluetoothMessage(
        context: Context
    ) {

        val location = weatherState.value.location
        val conditions = weatherState.value.conditions

        //Main json object that is sent through ble connection
        val jsonToSend = JSONObject()

        jsonToSend.put("command", "w")
        jsonToSend.put("location", location.name) //location name

        //Json array for the list of conditions
        val jsonArray = JSONArray()

        val currCond = weatherState.value.conditions.find { it.isCurrent }

        if (currCond != null) {

            //Current condition
            jsonArray.put(
                conditionToJsonObject(
                    time = "Now",
                    temperature = currCond.temp,
                    wind = currCond.windSpeed,
                    //In order to get the image's name instead of the android identifier of the resource
                    iconName = context.resources.getResourceEntryName(currCond.iconId),
                    pressure = currCond.pressure
                )
            )

            // Forecasts. The number of forecasts sent via BLE depends on Constants.FORECASTS_TO_SEND_WITH_BLE.
            // To adjust how many are included, just change that constant.
            conditions.filter { !it.isCurrent }.take(Constants.FORECASTS_TO_SEND_WITH_BLE)
                .forEach { condition ->

                    //In order to get the image's name instead of the android identifier of the resource
                    val iconName = context.resources.getResourceEntryName(condition.iconId)

                    //Time formatter
                    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

                    val jsonCond = conditionToJsonObject(
                        time = timeFmt.format(condition.dateTime),
                        temperature = condition.temp,
                        wind = condition.windSpeed,
                        iconName = iconName,
                        pressure = condition.pressure
                    )

                    jsonArray.put(jsonCond)
                }

            jsonToSend.put("conditions", jsonArray)

            val msg = jsonToSend.toString()

            Log.d(TAG, "BLE message:\n$msg")

            if (isExtDeviceConnected) {
                viewModelScope.launch {
                    proxy.send(msg)
                }
            } else {
                Log.d(TAG, "External device not connected")
            }
        }
    }

    private fun conditionToJsonObject(
        time: String,
        temperature: Int,
        wind: Float,
        iconName: String,
        pressure: Int
    ): JSONObject {
        val json = JSONObject()

        json.put("time", time)
        json.put("temperature", temperature)
        json.put("wind", wind)
        json.put("iconName", iconName)
        json.put("pressure", pressure)

        return json
    }

    fun getGeolocationPermissions(context: Context): Map<String, Boolean> {
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(
            context,
            ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return mapOf(
            Pair(ACCESS_COARSE_LOCATION, hasCoarseLocationPermission), Pair(
                ACCESS_FINE_LOCATION, hasFineLocationPermission
            )
        )
    }

    private fun setStateAndCreateSnapshot(
        newCurrentCondition: APIWeatherCondition,
        newForecasts: APIWeatherForecasts,
        newGeolocationFlag: Boolean? = null,
        newLocation: WeatherLocation? = null
    ) {

        val newConditions: List<WeatherCondition> = listOf(
            WeatherCondition(
                newCurrentCondition.weather.main,
                newCurrentCondition.weather.description,
                newCurrentCondition.weather.id,
                newCurrentCondition.weather.icon,
                newCurrentCondition.main.temp,
                newCurrentCondition.main.feels_like,
                newCurrentCondition.main.temp_min,
                newCurrentCondition.main.temp_max,
                newCurrentCondition.wind.speed,
                newCurrentCondition.main.pressure,
                newCurrentCondition.main.humidity,
                newCurrentCondition.sys.sunrise,
                newCurrentCondition.sys.sunset,
                newCurrentCondition.dt,
                true
            )
        ) +
                newForecasts.list.map { forecast ->
                    WeatherCondition(
                        forecast.weather.main,
                        forecast.weather.description,
                        forecast.weather.id,
                        forecast.weather.icon,
                        forecast.main.temp,
                        forecast.main.feels_like,
                        forecast.main.temp_min,
                        forecast.main.temp_max,
                        forecast.wind.speed,
                        forecast.main.pressure,
                        forecast.main.humidity,
                        null, //No sunrise/sunset in forecasts
                        null,
                        forecast.dt,
                        false
                    )
                }

        applyState(
            newConditions = newConditions,
            newLocation = newLocation,
            newGeolocationFlag = newGeolocationFlag,
            newSelectedDay = startOfDay(getMinDateOfAvailableConditions(newConditions))
        )

        //Save data into the cache (only for geolocation data)
        if (weatherState.value.geolocationEnabled) {
            saveWeatherSnapshotIntoCache()
        }
    }

    /**
     * Returns the earliest (minimum) date among all available conditions' date.
     *
     * @throws IllegalStateException if no conditions are available.
     */
    private fun getMinDateOfAvailableConditions(conditions: List<WeatherCondition>): Date {
        val date = conditions.map { condition -> condition.dateTime }.toList().minOrNull()

        if (date != null) {
            return date
        }
        Log.d(TAG, "no conditions in the list")
        throw IllegalStateException()
    }

    /**
     * It allows to compare Date objects just by day, month and year (setting all the other parameters to 0)
     */
    fun startOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    fun hideNoResult() {
        showNoResults = false
    }

    private fun showErrorMessage(message: String) {
        errorMessage = message
    }

    fun hideErrorMessage() {
        errorMessage = ""
    }

    private suspend fun fetchCurrentWeatherInfo(
        lat: String,
        lon: String
    ): APIResult<APIWeatherCondition> {
        return when (val weatherInfo = repository.getCurrentWeather(lat, lon)) {
            is APIResult.Success -> {
                weatherInfo
            }

            is APIResult.GenericError -> {
                Log.d(TAG, "Error (code ${weatherInfo.code}): ${weatherInfo.error}")
                showErrorMessage(Constants.ERROR_GENERIC_CURRENT_WEATHER)
                weatherInfo
            }

            is APIResult.NetworkError -> {
                Log.d(TAG, "Network error")
                showErrorMessage(Constants.ERROR_NETWORK_CURRENT_WEATHER)
                weatherInfo
            }
        }
    }

    private suspend fun fetchForecastsInfo(
        lat: String,
        lon: String
    ): APIResult<APIWeatherForecasts> {
        return when (val forecasts = repository.getWeatherForecasts(lat, lon)) {
            is APIResult.Success -> {
                forecasts
            }

            is APIResult.GenericError -> {
                Log.d(TAG, "Error (code ${forecasts.code}): ${forecasts.error}")
                showErrorMessage(Constants.ERROR_GENERIC_FORECASTS)
                forecasts
            }

            is APIResult.NetworkError -> {
                Log.d(TAG, "Network error")
                showErrorMessage(Constants.ERROR_NETWORK_FORECASTS)
                forecasts
            }
        }
    }

    private suspend fun fetchLatLonByQuery(query: String): APIResult<List<WeatherLocation>> {
        return when (val locations = repository.searchLocations(query)) {
            is APIResult.Success -> {
                locations
            }

            is APIResult.GenericError -> {
                Log.d(TAG, "Error (code ${locations.code}): ${locations.error}")
                showErrorMessage(Constants.ERROR_GENERIC_LOCATIONS)
                locations
            }

            is APIResult.NetworkError -> {
                Log.d(TAG, "Network error")
                showErrorMessage(Constants.ERROR_NETWORK_LOCATIONS)
                locations
            }
        }
    }


    @SuppressLint("MissingPermission")
    private suspend fun fetchGeolocation(
        fusedLocationClient: FusedLocationProviderClient,
        context: Context
    ): GeolocationResult {
        if (getGeolocationPermissions(context).values.none { it }) {
            return GeolocationResult.NoPermissionGranted
        }

        return suspendCancellableCoroutine { continuation ->
            val permissions = getGeolocationPermissions(context)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { lastLocation: Location? ->

                    if (lastLocation != null && (System.currentTimeMillis() - lastLocation.time) <= Constants.MAX_AGE_LAST_LOCATION) {
                        //there is a last location saved and it is not too old
                        continuation.resume(GeolocationResult.Success(lastLocation))
                    } else {
                        val priority: Int = when {
                            permissions.getOrDefault(
                                ACCESS_FINE_LOCATION,
                                false
                            ) -> Priority.PRIORITY_HIGH_ACCURACY

                            permissions.getOrDefault(
                                ACCESS_COARSE_LOCATION,
                                false
                            ) -> Priority.PRIORITY_BALANCED_POWER_ACCURACY //FIXME: loading is too long
                            else -> {
                                continuation.resume(GeolocationResult.NoPermissionGranted)
                                return@addOnSuccessListener
                            }
                        }

                        //fetch the current location
                        fusedLocationClient.getCurrentLocation(priority, null)
                            .addOnSuccessListener { currentLocation: Location? ->

                                if (currentLocation != null) {
                                    continuation.resume(
                                        GeolocationResult.Success(
                                            currentLocation
                                        )
                                    )
                                } else {
                                    continuation.resume(GeolocationResult.NotAvailable)
                                }
                            }
                            .addOnFailureListener { exception ->

                                if (continuation.isActive) {
                                    continuation.resume(GeolocationResult.Error(exception))
                                }
                            }
                    }

                }
                .addOnFailureListener { exception ->

                    if (continuation.isActive) {
                        continuation.resume(GeolocationResult.Error(exception))
                    }
                }
        }
    }

    private fun getWeatherByResult(result: WeatherLocation) {
        viewModelScope.launch {

            //get weather infos of the result
            when (val newCurrentWeatherCondition =
                fetchCurrentWeatherInfo(result.lat, result.lon)) {
                is APIResult.Success -> {
                    //update weather and location states

                    when (val newForecasts = fetchForecastsInfo(result.lat, result.lon)) {
                        is APIResult.Success -> {

                            setStateAndCreateSnapshot(
                                newCurrentCondition = newCurrentWeatherCondition.value,
                                newForecasts = newForecasts.value,
                                newGeolocationFlag = false,
                                newLocation = WeatherLocation(
                                    newCurrentWeatherCondition.value.name,
                                    newCurrentWeatherCondition.value.coord.lat,
                                    newCurrentWeatherCondition.value.coord.lon,
                                    newCurrentWeatherCondition.value.sys.country,
                                    result.state.orEmpty()
                                )
                            )
                        }

                        else -> {
                            //all the other cases already handled
                        }
                    }
                }

                else -> {
                    //all the other cases already handled
                }
            }
        }
    }

// Functions called by the UI (e.g. onClick handlers)

    fun refreshWeatherInfos(
        fusedLocationClient: FusedLocationProviderClient,
        context: Context
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing = true
            if (weatherState.value.geolocationEnabled) {

                //fetch geolocation
                when (val geo = fetchGeolocation(fusedLocationClient, context)) {

                    is GeolocationResult.Success -> {

                        //update weather conditions and location
                        when (val newCurrentWeatherCondition =
                            fetchCurrentWeatherInfo(
                                geo.data.latitude.toString(),
                                geo.data.longitude.toString()
                            )) {

                            is APIResult.Success -> {

                                when (val newForecasts = fetchForecastsInfo(
                                    geo.data.latitude.toString(),
                                    geo.data.longitude.toString()
                                )) {
                                    is APIResult.Success -> {

                                        // state not available with this API call
                                        setStateAndCreateSnapshot(
                                            newCurrentCondition = newCurrentWeatherCondition.value,
                                            newForecasts = newForecasts.value,
                                            newLocation = WeatherLocation(
                                                newCurrentWeatherCondition.value.name,
                                                newCurrentWeatherCondition.value.coord.lat,
                                                newCurrentWeatherCondition.value.coord.lon,
                                                newCurrentWeatherCondition.value.sys.country,
                                                ""
                                            )
                                        )

                                        val currentWeatherConditionState =
                                            weatherState.value.conditions.find { it.isCurrent }

                                        if (currentWeatherConditionState != null) {

                                            //send updates to ESP (just the current geolocation condition) //TODO
                                            sendBluetoothMessage(context = context)
                                        }
                                    }

                                    else -> {
                                        //all the other cases already handled
                                    }
                                }
                            }

                            else -> {
                                //all the other cases already handled
                            }
                        }
                    }

                    is GeolocationResult.NotAvailable -> {
                        showErrorMessage(Constants.ERROR_GEOLOCATION_NOT_AVAILABLE)
                    }

                    is GeolocationResult.NoPermissionGranted -> {
                        showErrorMessage(Constants.ERROR_GEOLOCATION_NO_PERMISSIONS)
                    }

                    is GeolocationResult.Error -> {
                        Log.d(TAG, "Error: ${geo.exception.message.orEmpty()}")
                        showErrorMessage(Constants.ERROR_GEOLOCATION_GENERIC)
                    }
                }
            } else {
                when (val newCurrentWeatherCondition =
                    fetchCurrentWeatherInfo(
                        weatherState.value.location.lat,
                        weatherState.value.location.lon
                    )) {
                    is APIResult.Success -> {
                        when (val newForecasts = fetchForecastsInfo(
                            weatherState.value.location.lat,
                            weatherState.value.location.lon
                        )) {
                            is APIResult.Success -> {

                                setStateAndCreateSnapshot(
                                    newCurrentWeatherCondition.value,
                                    newForecasts.value
                                )
                            }

                            else -> {
                                //all the other cases already handled
                            }
                        }
                    }

                    else -> {
                        //all the other cases already handled
                    }
                }
            }
            isRefreshing = false
        }
    }

    fun getGeolocationWeather(
        fusedLocationClient: FusedLocationProviderClient,
        context: Context
    ) {
        isLoading = true
        viewModelScope.launch(Dispatchers.IO) {

            //Fetch cached data before asking new geolocation infos
            val isCacheValid = tryLoadDataFromCache()

            if (!isCacheValid) {
                when (val geo = fetchGeolocation(fusedLocationClient, context)) {

                    is GeolocationResult.Success -> {
                        //update weather conditions
                        when (val newCurrentWeatherCondition =
                            fetchCurrentWeatherInfo(
                                geo.data.latitude.toString(),
                                geo.data.longitude.toString()
                            )) {

                            is APIResult.Success -> {

                                when (val newForecasts = fetchForecastsInfo(
                                    geo.data.latitude.toString(),
                                    geo.data.longitude.toString()
                                )) {
                                    is APIResult.Success -> {

                                        // state not available with this API call
                                        setStateAndCreateSnapshot(
                                            newCurrentCondition = newCurrentWeatherCondition.value,
                                            newForecasts = newForecasts.value,
                                            newGeolocationFlag = true,
                                            newLocation = WeatherLocation(
                                                newCurrentWeatherCondition.value.name,
                                                newCurrentWeatherCondition.value.coord.lat,
                                                newCurrentWeatherCondition.value.coord.lon,
                                                newCurrentWeatherCondition.value.sys.country,
                                                ""
                                            )
                                        )

                                        val currentWeatherConditionState =
                                            weatherState.value.conditions.find { it.isCurrent }

                                        if (currentWeatherConditionState != null) {

                                            //send updates to ESP (just the current geolocation condition) //TODO
                                            sendBluetoothMessage(context = context)
                                        }
                                    }

                                    else -> {
                                        //all the other cases already handled
                                    }
                                }
                            }

                            else -> {
                                //all the other cases already handled
                            }
                        }
                    }

                    is GeolocationResult.NotAvailable -> {
                        showErrorMessage(Constants.ERROR_GEOLOCATION_NOT_AVAILABLE)
                    }

                    is GeolocationResult.NoPermissionGranted -> {
                        showErrorMessage(Constants.ERROR_GEOLOCATION_NO_PERMISSIONS)
                    }

                    is GeolocationResult.Error -> {
                        Log.d(TAG, "Error: ${geo.exception.message.orEmpty()}")
                        showErrorMessage(Constants.ERROR_GEOLOCATION_GENERIC)
                    }
                }
            } else {
                val currentWeatherConditionState =
                    weatherState.value.conditions.find { it.isCurrent }

                if (currentWeatherConditionState != null) {

                    //send updates to ESP (just the current geolocation condition) //TODO
                    sendBluetoothMessage(context = context)
                }
            }
            isLoading = false
        }
    }

    fun getWeatherOfSelectedLocation(result: WeatherLocation) {
        getWeatherByResult(result)
    }

    fun searchLocations(query: String) {
        viewModelScope.launch {
            when (val locations = fetchLatLonByQuery(query)) {
                is APIResult.Success -> {
                    _searchedLocations.clear()
                    _searchedLocations.addAll(locations.value)

                    showNoResults = true
                }

                else -> {
                    //all the other cases already handled
                }
            }
        }
    }

    fun changeSelectedDay(newDate: Date) {
        applyState(
            newSelectedDay = startOfDay(newDate)
        )
    }
}