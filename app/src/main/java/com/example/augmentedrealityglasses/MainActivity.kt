package com.example.augmentedrealityglasses

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.augmentedrealityglasses.ble.device.BleDevice
import com.example.augmentedrealityglasses.ble.device.BleManager
import com.example.augmentedrealityglasses.ble.permissions.BluetoothSampleBox
import com.example.augmentedrealityglasses.screens.ConnectScreen
import com.example.augmentedrealityglasses.screens.ConnectViewModel
import com.example.augmentedrealityglasses.screens.FindDeviceScreen
import com.example.augmentedrealityglasses.screens.ScreenName
import com.example.augmentedrealityglasses.screens.TranslationScreen
import com.example.augmentedrealityglasses.screens.TranslationViewModel
import com.example.augmentedrealityglasses.screens.WeatherScreen

class MainActivity : ComponentActivity() {
    private val TAG = "myapp"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val bleManager = BleManager()
            NavHost(navController = navController, startDestination = ScreenName.HOME.name) {
                composable(ScreenName.HOME.name) {
                    HomeScreen {
                        navController.navigate(ScreenName.FIND_DEVICE.name)
                    }
                }
                composable(ScreenName.FIND_DEVICE.name) {
                    BluetoothSampleBox {
                        FindDeviceScreen(onError = {
                            Log.d(TAG, "Error occurred during scanning")
                            navController.navigate(ScreenName.ERROR_SCREEN.name)
                        })  {
                            device ->
                                Log.d(TAG, "device has been found")
                                bleManager.setDataSource(BleDevice(device))
                                navController.navigate(ScreenName.CONNECT_SCREEN.name)
                        }
                    }
                }
                composable(ScreenName.CONNECT_SCREEN.name) {
                    val viewModel = ConnectViewModel(bleManager)
                    viewModel.connect(LocalContext.current)

                    ConnectScreen(
                        viewModel,
                        onClickFeature = {
                            screen ->
                                navController.navigate(screen)
                        }
                    ) {
                        Log.d(TAG, "Connection closed")
                        bleManager.close()
                    }

                }
                composable (ScreenName.WEATHER_SCREEN.name) {
                    // TODO: add function by Teo
                    WeatherScreen()
                }

                composable(ScreenName.TRANSLATION_SCREEN.name) {
                    // TODO: integrate with the application by Ale
                    val translationViewModel =
                        TranslationViewModel(bleManager)
                    TranslationScreen(
                        translationViewModel,
                        onSendingData = {
                            msg ->
                                translationViewModel.send(msg)
                        }
                    )
                }

                composable(ScreenName.ERROR_SCREEN.name) {
                    ErrorScreen()
                }
            }
        }
    }
}