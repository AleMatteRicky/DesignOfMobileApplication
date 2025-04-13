package com.example.augmentedrealityglasses

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = ScreenName.HOME.name) {
                composable(ScreenName.HOME.name) {
                    checkRecordAudioPermission()
                }
            }
        }
    }

    @Composable
    fun checkRecordAudioPermission(){
        if(audioPermissionGranted()){
            //program regularly shows translation feature screen
        }
        else{
            requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 10) //todo replace 10 with a constant
            //graceful degrade the translation feature
            //override onRequestPermissionsResult with code related to this permission
        }
    }

    private fun audioPermissionGranted(): Boolean{
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}