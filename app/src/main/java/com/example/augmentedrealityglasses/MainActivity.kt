package com.example.augmentedrealityglasses

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.example.augmentedrealityglasses.ble.permissions.BluetoothSampleBox
import com.example.augmentedrealityglasses.ble.screens.ConnectScreen
import com.example.augmentedrealityglasses.ble.screens.FindDeviceScreen
import com.example.augmentedrealityglasses.ble.viewmodels.ConnectViewModel
import com.example.augmentedrealityglasses.notifications.permissions.PermissionsForNotification
import com.example.augmentedrealityglasses.settings.SettingsScreen
import com.example.augmentedrealityglasses.settings.SettingsViewModel
import com.example.augmentedrealityglasses.settings.ThemeMode
import com.example.augmentedrealityglasses.translation.TranslationViewModel
import com.example.augmentedrealityglasses.translation.permission.PermissionsForTranslation
import com.example.augmentedrealityglasses.translation.ui.TranslationHomeScreen
import com.example.augmentedrealityglasses.translation.ui.TranslationLanguageSelectionScreen
import com.example.augmentedrealityglasses.translation.ui.TranslationResultScreen
import com.example.augmentedrealityglasses.weather.screen.SearchLocationsScreen
import com.example.augmentedrealityglasses.weather.screen.WeatherScreen
import com.example.augmentedrealityglasses.weather.viewmodel.WeatherViewModel

class MainActivity : ComponentActivity() {
    private val TAG = "myActivity"

    @SuppressLint("UnrememberedGetBackStackEntry")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as App
        setContent {
            val navController = rememberNavController()
            val configuration = LocalConfiguration.current
            val screenHeight = configuration.screenHeightDp.dp

            val navigationBarHeight = screenHeight * 0.111f //could be a global variable
            val navigationBarVisible = remember { mutableStateOf(true) }

            //Loading the user settings
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
            LaunchedEffect(Unit) {
                //Try to load settings from cache
                settingsViewModel.loadSettings()
            }
            val settingsUi by settingsViewModel.uiState.collectAsStateWithLifecycle()

            //Use this flag in order to show properly the content of the screen
            val isDarkThemeSelected = when (settingsUi.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK   -> true
                ThemeMode.LIGHT  -> false
            }

            Scaffold(
                bottomBar = {
                    AnimatedVisibility(
                        //todo check if with other px slideVertically works without glitch
                        //Slide moves the component but it does not change its dimension so in order
                        //to have the other elements that follow the vertical slide we need shrink and expand

                        //todo try to check if it is possible to move the other components down less than the navbar size in order to distantiate more record button from language selection
                        visible = navigationBarVisible.value,
                        enter = slideInVertically { 0 } + expandVertically(
                            expandFrom = Alignment.Top
                        ) + fadeIn(
                            initialAlpha = 0.3f
                        ),
                        exit = slideOutVertically { fullHeight -> fullHeight } + shrinkVertically() + fadeOut()
                    ) {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        if (currentRoute !in listOf(
                                ScreenName.TRANSLATION_RESULT_SCREEN.name,
                                ScreenName.TRANSLATION_LANGUAGE_SELECTION_SCREEN.name,
                                ScreenName.WEATHER_SEARCH_LOCATIONS.name,
                                ScreenName.FIND_DEVICE.name
                            )
                        ) { //Screens in which navBar should be never shown
                            BottomNavigationBar(
                                navController,
                                Modifier
                                    .fillMaxWidth()
                                    .height(navigationBarHeight)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "HOME_GRAPH",
                    modifier = Modifier.padding(innerPadding)
                ) {

                    navigation(
                        startDestination = ScreenName.HOME.name,
                        route = "HOME_GRAPH"
                    ) {
                        //FIXME: create composable function for redundant code
                        composable(ScreenName.HOME.name) { backStackEntry ->

                            val bluetoothManager: BluetoothManager =
                                checkNotNull(applicationContext.getSystemService(BluetoothManager::class.java))
                            val adapter: BluetoothAdapter? = bluetoothManager.adapter

                            // TODO: make the control at the beginning not making clickable the icon in case bluetooth is not supported, instead of checking it here
                            require(adapter != null) {
                                "Bluetooth must be supported by this device"
                            }

                            val extras = MutableCreationExtras().apply {
                                set(HomeViewModel.ADAPTER_KEY, adapter)
                                set(APPLICATION_KEY, application)
                            }

                            val parentEntry = remember(backStackEntry) {
                                navController.getBackStackEntry("HOME_GRAPH")
                            }
                            val viewModel = viewModel<HomeViewModel>(
                                viewModelStoreOwner = parentEntry,
                                factory = HomeViewModel.Factory,
                                extras = extras
                            )

                            PermissionsForNotification(
                                app.container.isDeviceSmsCapable,
                                content = {
                                    HomeScreen(
                                        viewModel = viewModel,
                                        onNavigateFindDevice = { navController.navigate(ScreenName.FIND_DEVICE.name) }
                                    )
                                }
                            )
                        }
                        composable(ScreenName.FIND_DEVICE.name) { backStackEntry ->

                            BluetoothSampleBox {
                                val bluetoothManager: BluetoothManager =
                                    checkNotNull(
                                        applicationContext.getSystemService(
                                            BluetoothManager::class.java
                                        )
                                    )
                                val adapter: BluetoothAdapter? = bluetoothManager.adapter

                                // TODO: make the control at the beginning not making clickable the icon in case bluetooth is not supported, instead of checking it here
                                require(adapter != null) {
                                    "Bluetooth must be supported by this device"
                                }

                                val extras = MutableCreationExtras().apply {
                                    set(HomeViewModel.ADAPTER_KEY, adapter)
                                    set(APPLICATION_KEY, application)
                                }

                                val parentEntry = remember(backStackEntry) {
                                    navController.getBackStackEntry("HOME_GRAPH")
                                }
                                val viewModel = viewModel<HomeViewModel>(
                                    viewModelStoreOwner = parentEntry,
                                    factory = HomeViewModel.Factory,
                                    extras = extras
                                )

                                FindDeviceScreen(
                                    viewModel = viewModel,
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
//                    composable(ScreenName.TRANSLATION_HOME_SCREEN.name) {
//                        CheckRecordAudioPermission() //todo check if it works when permission are refused 1 time
//                        TranslationScreen(
//                            onNavigateToHome = {
//                                navController.navigate(
//                                    route = ScreenName.HOME.name
//                                )
//                            },
//                            viewModel = viewModel(factory = TranslationViewModel.Factory),
//                            enabled = translationFeatureAvailable(),
//                            navigationBarVisible = navigationBarVisible,
//                            navigationBarHeight = navigationBarHeight
//
//                        ) //todo update with system language from settings
//                    }

                    navigation(
                        startDestination = ScreenName.TRANSLATION_HOME_SCREEN.name,
                        route = "TRANSLATION_GRAPH"
                    ) {
                        composable(ScreenName.TRANSLATION_HOME_SCREEN.name) { backStackEntry ->
                            val parentEntry = remember(backStackEntry) {
                                navController.getBackStackEntry("TRANSLATION_GRAPH")
                            }
                            val viewModel = viewModel<TranslationViewModel>(
                                viewModelStoreOwner = parentEntry,
                                factory = TranslationViewModel.Factory
                            )

                            PermissionsForTranslation(isMicrophoneAvailable = isMicrophoneAvailable()) {
                                TranslationHomeScreen(
                                    onNavigateToHome = {
                                        navController.navigate(
                                            route = ScreenName.HOME.name
                                        )
                                    },
                                    onNavigateToResult = {
                                        navController.navigate(ScreenName.TRANSLATION_RESULT_SCREEN.name)
                                    },
                                    onNavigateToLanguageSelection = {
                                        navController.navigate(ScreenName.TRANSLATION_LANGUAGE_SELECTION_SCREEN.name)
                                    },
                                    onBack = {
                                        navController.popBackStack()
                                    },
                                    viewModel = viewModel,
                                    enabled = translationFeatureAvailable(),
                                    navigationBarVisible = navigationBarVisible,
                                    navigationBarHeight = navigationBarHeight

                                ) //todo update with system language from settings
                            }
                        }

                        composable(ScreenName.TRANSLATION_RESULT_SCREEN.name) { backStackEntry ->
                            val parentEntry = remember(backStackEntry) {
                                navController.getBackStackEntry("TRANSLATION_GRAPH")
                            }
                            val viewModel = viewModel<TranslationViewModel>(
                                viewModelStoreOwner = parentEntry,
                                factory = TranslationViewModel.Factory
                            )

                            TranslationResultScreen(
                                viewModel = viewModel,
                                onBack = {
                                    viewModel.clearText()
                                    navController.popBackStack()
                                }, onNavigateToLanguageSelection = {
                                    navController.navigate(ScreenName.TRANSLATION_LANGUAGE_SELECTION_SCREEN.name)
                                }
                            )
                        }

                        composable(ScreenName.TRANSLATION_LANGUAGE_SELECTION_SCREEN.name) { backStackEntry ->
                            val parentEntry = remember(backStackEntry) {
                                navController.getBackStackEntry("TRANSLATION_GRAPH")
                            }
                            val viewModel = viewModel<TranslationViewModel>(
                                viewModelStoreOwner = parentEntry,
                                factory = TranslationViewModel.Factory
                            )

                            TranslationLanguageSelectionScreen(
                                viewModel = viewModel,
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                    }

                    //TODO: prefetch data
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
                        SettingsScreen(
                            viewModel = settingsViewModel
                        )
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