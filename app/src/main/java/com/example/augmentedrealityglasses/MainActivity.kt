package com.example.augmentedrealityglasses

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.example.augmentedrealityglasses.ble.screens.ConnectScreen
import com.example.augmentedrealityglasses.ble.viewmodels.ConnectViewModel
import com.example.augmentedrealityglasses.settings.SettingsScreen
import com.example.augmentedrealityglasses.settings.SettingsViewModel
import com.example.augmentedrealityglasses.settings.ThemeMode
import com.example.augmentedrealityglasses.translation.TranslationViewModel
import com.example.augmentedrealityglasses.translation.ui.TranslationHomeScreen
import com.example.augmentedrealityglasses.translation.ui.TranslationLanguageSelectionScreen
import com.example.augmentedrealityglasses.translation.ui.TranslationResultScreen
import com.example.augmentedrealityglasses.weather.screen.SearchLocationsScreen
import com.example.augmentedrealityglasses.weather.screen.WeatherScreen
import com.example.augmentedrealityglasses.weather.viewmodel.WeatherViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject

//TODO: delete all unused files/composables/...
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
            val settingsViewModel: SettingsViewModel =
                viewModel(factory = SettingsViewModel.Factory)

            val settingsUi by settingsViewModel.uiState.collectAsStateWithLifecycle()

            //Use this flag in order to show properly the content on the screen
            val isDarkThemeSelected = when (settingsUi.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            AppTheme(
                isDarkThemeSelected = isDarkThemeSelected
            ) {
                Scaffold(
                    bottomBar = {
                        AnimatedVisibility(

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

                    val screenTransitionDuration = 150

                    NavHost(
                        navController = navController,
                        startDestination = "HOME_GRAPH",
                        enterTransition = { fadeIn(animationSpec = tween(screenTransitionDuration)) },
                        exitTransition = { fadeOut(animationSpec = tween(screenTransitionDuration)) },
                        modifier = Modifier.padding(innerPadding)
                    ) {

                        navigation(
                            startDestination = ScreenName.HOME.name,
                            route = "HOME_GRAPH"
                        ) {
                            composable(
                                ScreenName.HOME.name,
                                enterTransition = {
                                    fadeIn(
                                        animationSpec = tween(
                                            screenTransitionDuration
                                        )
                                    )
                                },
                                exitTransition = {
                                    fadeOut(
                                        animationSpec = tween(
                                            screenTransitionDuration
                                        )
                                    )
                                },
                            ) { backStackEntry ->

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

                                // If we derive physical location from BT devices or if the device runs on Android 11 or below
                                // we need location permissions otherwise we don't need to request them (see AndroidManifest).
                                val locationPermission: Set<String> =
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                        setOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                        )
                                    } else {
                                        emptySet()
                                    }

                                // For Android 12 and above we only need connect and scan
                                val bluetoothPermissionSet: Set<String> =
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        setOf(
                                            Manifest.permission.BLUETOOTH_CONNECT,
                                            Manifest.permission.BLUETOOTH_SCAN,
                                        )
                                    } else {
                                        setOf(
                                            Manifest.permission.BLUETOOTH,
                                            Manifest.permission.BLUETOOTH_ADMIN,
                                        )
                                    }

                                val homePermissions: Map<String, Boolean> = buildMap {
                                    put(Manifest.permission.READ_PHONE_STATE, true)
                                    put(Manifest.permission.READ_CALL_LOG, true)
                                    put(Manifest.permission.READ_CONTACTS, false)

                                    if (app.container.isDeviceSmsCapable) {
                                        put(Manifest.permission.RECEIVE_SMS, true)
                                    }

                                    putAll((locationPermission + bluetoothPermissionSet).associateWith { true })
                                }

                                val bluetoothAdapter =
                                    applicationContext.getSystemService<BluetoothManager>()?.adapter

                                // Check to see if the BLE feature is available.
                                val hasBLE =
                                    packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

                                //Check if the adapter is enabled
                                var isBTEnabled by remember {
                                    mutableStateOf(bluetoothAdapter?.isEnabled == true)
                                }

                                //Update in real time isBTEnabled flag
                                DisposableEffect(Unit) {
                                    val receiver = object : BroadcastReceiver() {
                                        override fun onReceive(ctx: Context?, intent: Intent?) {
                                            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                                                val state = intent.getIntExtra(
                                                    BluetoothAdapter.EXTRA_STATE,
                                                    BluetoothAdapter.ERROR
                                                )
                                                isBTEnabled = (state == BluetoothAdapter.STATE_ON)
                                            }
                                        }
                                    }

                                    val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        applicationContext.registerReceiver(
                                            receiver,
                                            filter,
                                            Context.RECEIVER_NOT_EXPORTED
                                        )
                                    } else {
                                        applicationContext.registerReceiver(receiver, filter)
                                    }
                                    onDispose { applicationContext.unregisterReceiver(receiver) }
                                }

                                //TODO: refine title and message
                                PermissionsBox(
                                    title = "Welcome",
                                    message = "In order to communicate with the external device, you need to grant these permissions",
                                    permissionsRequired = homePermissions,
                                    onSatisfied = {}
                                ) {
                                    if (!hasBLE) {
                                        BLENotSupportedScreen()
                                    } else {
                                        if (isBTEnabled) {
                                            HomeScreen(
                                                viewModel = viewModel,
                                                onNavigateFindDevice = {
                                                    navController.navigate(
                                                        ScreenName.FIND_DEVICE.name
                                                    )
                                                }, navigationBarHeight = navigationBarHeight
                                            )
                                        } else {
                                            BluetoothDisabledScreen {
                                                isBTEnabled = true
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        composable(
                            ScreenName.CONNECT_SCREEN.name,
                            enterTransition = {
                                fadeIn(
                                    animationSpec = tween(
                                        screenTransitionDuration
                                    )
                                )
                            },
                            exitTransition = {
                                fadeOut(
                                    animationSpec = tween(
                                        screenTransitionDuration
                                    )
                                )
                            },
                        ) {
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

                        navigation(
                            startDestination = ScreenName.TRANSLATION_HOME_SCREEN.name,
                            route = "TRANSLATION_GRAPH"
                        ) {
                            composable(
                                ScreenName.TRANSLATION_HOME_SCREEN.name,
                                enterTransition = {
                                    fadeIn(
                                        animationSpec = tween(screenTransitionDuration)
                                    )
                                },
                                exitTransition = {
                                    fadeOut(
                                        animationSpec = tween(
                                            screenTransitionDuration
                                        )
                                    )
                                },
                            ) { backStackEntry ->
                                val parentEntry = remember(backStackEntry) {
                                    navController.getBackStackEntry("TRANSLATION_GRAPH")
                                }
                                val viewModel = viewModel<TranslationViewModel>(
                                    viewModelStoreOwner = parentEntry,
                                    factory = TranslationViewModel.Factory
                                )

                                val permissionsForRecording: Map<String, Boolean> =
                                    if (isMicrophoneAvailable()) {
                                        mapOf(Pair(Manifest.permission.RECORD_AUDIO, true))
                                    } else {
                                        mapOf()
                                    }
                                //TODO: add screen for "microphone not available" case
                                PermissionsBox(
                                    message = "To enable speech-to-text transcription and translation, please grant microphone permission.",
                                    permissionsRequired = permissionsForRecording,
                                    iconId = Icon.MICROPHONE.getID(),
                                    onSatisfied = {}
                                ) {
                                    TranslationHomeScreen(
                                        onScreenComposition = {
                                            sendOpenTranslationBLEMessage()
                                        },
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

                                    )
                                }
                            }

                            composable(
                                ScreenName.TRANSLATION_RESULT_SCREEN.name,
                                enterTransition = {
                                    fadeIn(
                                        animationSpec = tween(screenTransitionDuration)
                                    )
                                },
                                exitTransition = {
                                    fadeOut(
                                        animationSpec = tween(
                                            screenTransitionDuration
                                        )
                                    )
                                },
                            ) { backStackEntry ->
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

                            composable(
                                ScreenName.TRANSLATION_LANGUAGE_SELECTION_SCREEN.name,
                                enterTransition = {
                                    fadeIn(
                                        animationSpec = tween(screenTransitionDuration)
                                    )
                                },
                                exitTransition = {
                                    fadeOut(
                                        animationSpec = tween(
                                            screenTransitionDuration
                                        )
                                    )
                                },
                            ) { backStackEntry ->
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

                        navigation(
                            startDestination = ScreenName.WEATHER_HOME_SCREEN.name,
                            route = "WEATHER_GRAPH"
                        ) {
                            composable(
                                ScreenName.WEATHER_HOME_SCREEN.name,
                                enterTransition = {
                                    fadeIn(
                                        animationSpec = tween(screenTransitionDuration)
                                    )
                                },
                                exitTransition = {
                                    fadeOut(
                                        animationSpec = tween(
                                            screenTransitionDuration
                                        )
                                    )
                                },
                            ) {
                                val parentEntry =
                                    remember { navController.getBackStackEntry("WEATHER_GRAPH") }
                                val viewModel = viewModel<WeatherViewModel>(
                                    viewModelStoreOwner = parentEntry,
                                    factory = WeatherViewModel.Factory
                                )

                                //TODO: refine title and message
                                PermissionsBox(
                                    permissionsRequired = mapOf(
                                        Pair(Manifest.permission.ACCESS_COARSE_LOCATION, false),
                                        Pair(Manifest.permission.ACCESS_FINE_LOCATION, false)
                                    ),
                                    iconId = R.drawable.location,
                                    message = "We need your location to show live local weather. Choose Precise or Approximate location (only one). You can update this in Settings at any time.",
                                    onSatisfied = {
                                        viewModel.hideErrorMessage()
                                    }
                                ) {
                                    WeatherScreen(
                                        onScreenComposition = {},
                                        onTextFieldClick = {
                                            navController.navigate(ScreenName.WEATHER_SEARCH_LOCATIONS.name)
                                        },
                                        viewModel = viewModel
                                    )
                                }
                            }

                            composable(
                                ScreenName.WEATHER_SEARCH_LOCATIONS.name,
                                enterTransition = {
                                    fadeIn(
                                        animationSpec = tween(screenTransitionDuration)
                                    )
                                },
                                exitTransition = {
                                    fadeOut(
                                        animationSpec = tween(
                                            screenTransitionDuration
                                        )
                                    )
                                },
                            ) {
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

                        composable(
                            ScreenName.SETTINGS.name,
                            enterTransition = {
                                fadeIn(
                                    animationSpec = tween(
                                        screenTransitionDuration
                                    )
                                )
                            },
                            exitTransition = {
                                fadeOut(
                                    animationSpec = tween(
                                        screenTransitionDuration
                                    )
                                )
                            },
                        ) {
                            SettingsScreen(
                                viewModel = settingsViewModel
                            )
                        }
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

    /**
     * This function creates a ble message that notifies to the external device the change of screen (used only for translation feature)
     */
    private fun sendOpenTranslationBLEMessage() {
        val proxy = (application as App).container.proxy

        //Main json object that is sent through ble connection
        val jsonToSend = JSONObject()

        jsonToSend.put("command", "t")
        jsonToSend.put("text", "") //Empty text, just to notify the screen change

        val msg = jsonToSend.toString()

        Log.d(TAG, "BLE message:\n$msg")

        if (proxy.isConnected()) {
            lifecycleScope.launch {
                proxy.send(msg)
            }
        } else {
            Log.d(TAG, "External device not connected")
        }
    }
}