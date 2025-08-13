package com.example.augmentedrealityglasses

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.example.augmentedrealityglasses.ble.permissions.BluetoothSampleBox
import com.example.augmentedrealityglasses.ble.screens.ConnectScreen
import com.example.augmentedrealityglasses.ble.screens.FindDeviceScreen
import com.example.augmentedrealityglasses.ble.viewmodels.ConnectViewModel
import com.example.augmentedrealityglasses.ble.viewmodels.FindDeviceViewModel
import com.example.augmentedrealityglasses.notifications.permissions.PermissionsForNotification
import com.example.augmentedrealityglasses.translation.TranslationViewModel
import com.example.augmentedrealityglasses.translation.ui.TranslationScreen
import com.example.augmentedrealityglasses.weather.screen.SearchLocationsScreen
import com.example.augmentedrealityglasses.weather.screen.WeatherScreen
import com.example.augmentedrealityglasses.weather.viewmodel.WeatherViewModel

class MainActivity : ComponentActivity() {
    private val TAG = "myActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as App
        setContent {
            val navController = rememberNavController()
            val configuration = LocalConfiguration.current
            val screenHeight = configuration.screenHeightDp.dp

            val navigationBarHeight = screenHeight * 0.111f //could be a global variable
            val navigationBarVisible = remember { mutableStateOf(true) }

            Scaffold(
                bottomBar = {
                    AnimatedVisibility(
                        visible = navigationBarVisible.value,
                        enter = slideInVertically(),
                        exit = slideOutVertically()
                    ) {
                        BottomNavigationBar(
                            navController,
                            Modifier
                                .fillMaxWidth()
                                .height(navigationBarHeight)
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = ScreenName.HOME.name,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(ScreenName.HOME.name) {
                        PermissionsForNotification(
                            app.container.isDeviceSmsCapable,
                            content = {
                                HomeScreen(
                                    onNavigateToTranslation = {
                                        navController.navigate(
                                            route = ScreenName.TRANSLATION_SCREEN.name
                                        )
                                    },
                                    onNavigateToWeather = { navController.navigate(ScreenName.WEATHER_HOME_SCREEN.name) },
                                    onNavigateToBLE = { navController.navigate(ScreenName.FIND_DEVICE.name) },
                                    onNavigateToConnect = {
                                        navController.navigate(ScreenName.CONNECT_SCREEN.name)
                                    }
                                )
                            }
                        )
                    }
                    composable(ScreenName.FIND_DEVICE.name) {
                        BluetoothSampleBox {
                            val bluetoothManager: BluetoothManager =
                                checkNotNull(applicationContext.getSystemService(BluetoothManager::class.java))
                            val adapter: BluetoothAdapter? = bluetoothManager.adapter

                            // TODO: make the control at the beginning not making clickable the icon in case bluetooth is not supported, instead of checking it here
                            require(adapter != null) {
                                "Bluetooth must be supported by this device"
                            }

                            val extras = MutableCreationExtras().apply {
                                set(FindDeviceViewModel.ADAPTER_KEY, adapter)
                                set(APPLICATION_KEY, application)
                            }

                            FindDeviceScreen(
                                viewModel = viewModel(
                                    factory = FindDeviceViewModel.Factory,
                                    extras = extras
                                ),
                                navigateOnError = {
                                    Log.d(TAG, "Error occurred during scanning")
                                    navController.navigate(ScreenName.ERROR_SCREEN.name)
                                },
                                navigateOnFeatures = {
                                    navController.navigate(ScreenName.HOME.name)
                                }
                            )
                        }
                    }
                    composable(ScreenName.CONNECT_SCREEN.name) {
                        ConnectScreen(
                            viewModel = viewModel(factory = ConnectViewModel.Factory),
                            onNavigateToFeature = { screen ->
                                navController.navigate(screen)
                            },
                            onNavigateAfterClosingTheConnection = {
                                navController.navigate(ScreenName.FIND_DEVICE.name) {
                                    popUpTo(ScreenName.CONNECT_SCREEN.name) { inclusive = true }
                                }
                            }
                        )
                    }
                    /*

                    composable(ScreenName.TRANSLATION_SCREEN.name) {
                        // TODO: integrate with the application by Ale
                        val translationViewModel: com.example.augmentedrealityglasses.screens.TranslationViewModel =
                            viewModel(factory = com.example.augmentedrealityglasses.screens.TranslationViewModel.Factory)
                        TranslationScreen(
                            translationViewModel = translationViewModel,
                            onSendingData = { msg ->
                                translationViewModel.send(msg)
                            }
                        )
                    }
                    */
                    composable(ScreenName.TRANSLATION_SCREEN.name) {
                        CheckRecordAudioPermission() //todo check if it works when permission are refused 1 time
                        TranslationScreen(
                            onNavigateToHome = {
                                navController.navigate(
                                    route = ScreenName.HOME.name
                                )
                            },
                            viewModel = viewModel(factory = TranslationViewModel.Factory),
                            enabled = translationFeatureAvailable(),
                            navigationBarVisible = navigationBarVisible,
                            navigationBarHeight = navigationBarHeight

                        ) //todo update with system language from settings
                    }

                    navigation(
                        startDestination = ScreenName.WEATHER_HOME_SCREEN.name,
                        route = "WEATHER_GRAPH"
                    ) {
                        composable(ScreenName.WEATHER_HOME_SCREEN.name) {
                            val parentEntry =
                                remember { navController.getBackStackEntry("WEATHER_GRAPH") }
                            val viewModel = viewModel<WeatherViewModel>(
                                viewModelStoreOwner = parentEntry,
                                factory = WeatherViewModel.Factory
                            )

                            WeatherScreen(
                                onTextFieldClick = {
                                    navController.navigate(ScreenName.WEATHER_SEARCH_LOCATIONS.name)
                                },
                                viewModel = viewModel
                            )
                        }

                        composable(ScreenName.WEATHER_SEARCH_LOCATIONS.name) {
                            val parentEntry =
                                remember { navController.getBackStackEntry("WEATHER_GRAPH") }
                            val viewModel = viewModel<WeatherViewModel>(
                                viewModelStoreOwner = parentEntry,
                                factory = WeatherViewModel.Factory
                            )

                            SearchLocationsScreen(
                                onBackClick = {
                                    navController.navigate(ScreenName.WEATHER_HOME_SCREEN.name)
                                },
                                viewModel = viewModel
                            )
                        }
                    }

                    //TODO
                    composable(ScreenName.SETTINGS.name) {
                        Text("Settings page")
                    }
                }
            }
        }
    }

    @Composable
    private fun CheckRecordAudioPermission() {
        if (!audioPermissionGranted()) {
            requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                10
            ) //todo replace 10 with a constant
            //graceful degrade the translation feature
            //override onRequestPermissionsResult with code related to this permission
        }
    }

    private fun audioPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isMicrophoneAvailable(): Boolean {
        return applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
    }

    private fun translationFeatureAvailable(): Boolean {
        return audioPermissionGranted() && isMicrophoneAvailable()
    }
}