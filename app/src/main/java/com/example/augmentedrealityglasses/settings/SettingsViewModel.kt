package com.example.augmentedrealityglasses.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.augmentedrealityglasses.App
import com.example.augmentedrealityglasses.BluetoothUpdateStatus
import com.example.augmentedrealityglasses.BootstrapPrefs
import com.example.augmentedrealityglasses.ble.devicedata.RemoteDeviceManager
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.ConnectionState
import com.example.augmentedrealityglasses.cache.Cache
import com.example.augmentedrealityglasses.cache.CachePolicy
import com.example.augmentedrealityglasses.cache.DefaultTimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(

    private val proxy: RemoteDeviceManager,
    private val bootstrapPrefs: BootstrapPrefs,
    private val cache: Cache,
    private val cachePolicy: CachePolicy
) : ViewModel() {

    var bluetoothUpdateStatus by mutableStateOf(BluetoothUpdateStatus.NONE)
        private set

    var isExtDeviceConnected by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf("")
        private set

    //Initialize the viewModel
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val bleManager =
                    (this[APPLICATION_KEY] as App).container.proxy
                val cache =
                    (this[APPLICATION_KEY] as App).container.settingsCache
                val policy =
                    (this[APPLICATION_KEY] as App).container.settingsCachePolicy
                val bootstrapPrefs =
                    (this[APPLICATION_KEY] as App).container.bootstrapPrefs
                SettingsViewModel(
                    proxy = bleManager,
                    cache = cache,
                    cachePolicy = policy,
                    bootstrapPrefs = bootstrapPrefs
                )
            }
        }
    }

    //Tag for logging
    private val TAG = "Settings_viewModel"

    private val CACHE_KEY = "settings"

    private val initialTheme = bootstrapPrefs.getTheme() ?: ThemeMode.SYSTEM

    private val _uiState = MutableStateFlow(
        SettingsUIState(
            theme = initialTheme,
            notificationEnabled = mapOf(
                Pair(NotificationSource.WHATSAPP, false),
                Pair(NotificationSource.TELEGRAM, false),
                Pair(NotificationSource.CALL, false),
                Pair(NotificationSource.SMS, false),
                Pair(NotificationSource.GMAIL, false),
                Pair(NotificationSource.OUTLOOK, false)
            )
        )
    )
    val uiState: StateFlow<SettingsUIState> = _uiState

    init {
        viewModelScope.launch {
            if (proxy.isConnected()) {
                proxy.receiveUpdates()
                    .collect { connectionState ->
                        if (connectionState.connectionState is ConnectionState.Connected) {
                            isExtDeviceConnected = true
                            bluetoothUpdateStatus = BluetoothUpdateStatus.DEVICE_CONNECTED
                        } else {
                            isExtDeviceConnected = false
                            bluetoothUpdateStatus = BluetoothUpdateStatus.DEVICE_DISCONNECTED
                        }
                    }
            }
        }

        viewModelScope.launch {
            loadSettings()
        }
    }

    private fun applyState(
        newThemeMode: ThemeMode? = null,
        updatedNotification: Pair<NotificationSource, Boolean>? = null
    ) {
        _uiState.update { old ->
            val updatedMap = old.notificationEnabled.toMutableMap().apply {
                updatedNotification?.let { (app, enabled) ->
                    put(app, enabled)
                }
            }.toMap()

            old.copy(
                theme = newThemeMode ?: old.theme,
                notificationEnabled = updatedMap
            )
        }

        saveSettingsSnapshotIntoCache()
        bootstrapPrefs.setTheme(_uiState.value.theme)
    }

    fun onSystemToggle(checked: Boolean) {
        if (checked) {
            applyState(ThemeMode.SYSTEM)
        } else {
            applyState(ThemeMode.LIGHT)
        }
    }

    fun onSelectLight() {
        applyState(ThemeMode.LIGHT)
    }

    fun onSelectDark() {
        applyState(ThemeMode.DARK)
    }

    private fun loadSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            tryLoadDataFromCache()
        }
    }

    fun onEnableNotificationSource(source: NotificationSource) {
        applyState(
            updatedNotification = Pair(source, true)
        )
    }

    fun onDisableNotificationSource(source: NotificationSource) {
        applyState(
            updatedNotification = Pair(source, false)
        )
    }

    fun hideErrorMessage() {
        errorMessage = ""
    }

    private fun saveSettingsSnapshotIntoCache() {
        viewModelScope.launch(Dispatchers.IO) {
            cache.set(
                key = CACHE_KEY,
                value = uiState.value,
                serializer = SettingsUIState.serializer(),
                timeProvider = DefaultTimeProvider
            )
        }
    }

    private suspend fun tryLoadDataFromCache(): Boolean {

        val state = withContext(Dispatchers.IO) {
            cache.getIfValid(
                key = CACHE_KEY,
                policy = cachePolicy,
                serializer = SettingsUIState.serializer(),
                timeProvider = DefaultTimeProvider
            )
        } ?: return false

        _uiState.update { old ->
            old.copy(
                theme = state.theme,
                notificationEnabled = state.notificationEnabled
            )
        }

        return true
    }

    fun hideBluetoothUpdate() {
        bluetoothUpdateStatus = BluetoothUpdateStatus.NONE
    }
}