package com.example.augmentedrealityglasses

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.augmentedrealityglasses.weather.screen.WeatherScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = ScreenName.HOME.name) {
                composable(ScreenName.HOME.name) {
                    Greeting(
                        navController = navController,
                        name = "Android"
                    )
                }
                composable(ScreenName.WEATHER.name) {
                    WeatherScreen()
                }
            }
        }
    }
}

@Composable
fun Greeting(navController: NavHostController, name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
    Button(onClick = { openWeather(navController) }, modifier = Modifier.padding(top = 20.dp)) {
        Text(
            text = "Weather"
        )
    }
}

fun openWeather(navController: NavHostController) {
    navController.navigate(ScreenName.WEATHER.name)
}