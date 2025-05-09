package com.example.augmentedrealityglasses

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.augmentedrealityglasses.ble.permissions.BluetoothSampleBox
import com.example.augmentedrealityglasses.ble.screens.ConnectScreen
import com.example.augmentedrealityglasses.ble.screens.FindDeviceScreen
import com.example.augmentedrealityglasses.ble.viewmodels.ConnectViewModel
import com.example.augmentedrealityglasses.ble.viewmodels.FindDeviceViewModel
import com.example.augmentedrealityglasses.notifications.permissions.PermissionsForNotification
import com.example.augmentedrealityglasses.translation.ui.TranslationScreen
import com.example.augmentedrealityglasses.weather.screen.WeatherScreen
import com.google.mlkit.nl.translate.TranslateLanguage

class MainActivity : ComponentActivity() {
    private val TAG = "myActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as App
        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = ScreenName.HOME.name) {
                composable(ScreenName.HOME.name) {
                    PermissionsForNotification(
                        app.container.isDeviceSmsCapable,
                        content={
                            HomeScreen(
                                onNavigateToTranslation = {
                                    navController.navigate(
                                        route = ScreenName.TRANSLATION_SCREEN.name
                                    )
                                },
                                onNavigateToWeather = { navController.navigate(ScreenName.WEATHER_SCREEN.name) },
                                onNavigateToBLE = { navController.navigate(ScreenName.FIND_DEVICE.name) }
                            )
                        }
                    )
                }
                composable(ScreenName.FIND_DEVICE.name) {
                    BluetoothSampleBox {
                        FindDeviceScreen(
                            viewModel = viewModel(factory = FindDeviceViewModel.Factory),
                            navigateOnError = {
                                Log.d(TAG, "Error occurred during scanning")
                                navController.navigate(ScreenName.ERROR_SCREEN.name)
                            },
                            navigateOnConnect = {
                                navController.navigate(ScreenName.CONNECT_SCREEN.name)
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
                        com.example.augmentedrealityglasses.translation.TranslationViewModel(
                            systemLanguage = TranslateLanguage.ITALIAN,
                            application
                        ), enabled = translationFeatureAvailable()
                    ) //todo update with system language from settings
                }
                composable(ScreenName.WEATHER_SCREEN.name) {
                    WeatherScreen()
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