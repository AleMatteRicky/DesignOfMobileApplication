package com.example.augmentedrealityglasses

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.augmentedrealityglasses.translation.TranslationViewModel
import com.example.augmentedrealityglasses.translation.ui.SelectLanguageButton
import com.google.mlkit.nl.translate.TranslateLanguage

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = ScreenName.HOME.name) {
                composable(ScreenName.HOME.name) {
                    HomeScreen(
                        onNavigateToTranslation = {
                            navController.navigate(
                                route = ScreenName.TRANSLATION.name
                            )
                        }
                    )
                }

                composable(ScreenName.TRANSLATION.name) {
                    CheckRecordAudioPermission() //todo check if it works when permission are refused 1 time
                    TranslationScreen(
                        onNavigateToHome = {
                            navController.navigate(
                                route = ScreenName.HOME.name
                            )
                        },
                        TranslationViewModel(
                            systemLanguage = TranslateLanguage.ITALIAN,
                            application
                        )
                    ) //todo update with system language from settings
                }
            }
        }
    }

    @Composable
    fun CheckRecordAudioPermission() {
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

    fun audioPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun HomeScreen(onNavigateToTranslation: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Button(onClick = {
            onNavigateToTranslation()
        }) {
            Text("Translation Screen")
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun TranslationScreen(onNavigateToHome: () -> Unit, viewModel: TranslationViewModel) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        var recordingButtonText by remember { mutableStateOf("Record") }
        Button(onClick = {
            if (viewModel.uiState.isRecording) {
                viewModel.stopRecording()
                recordingButtonText = "Record"
            } else {
                viewModel.startRecording()
                recordingButtonText = "Stop Recording"
            }
        }, modifier = Modifier.align(Alignment.Center)) {
            Text(recordingButtonText)
        }

        Text(
            text = viewModel.uiState.recognizedText,
            modifier = Modifier.align(Alignment.TopStart)
        )
        Text(
            text = viewModel.uiState.translatedText,
            modifier = Modifier.align(Alignment.BottomStart)
        )
        Box(
            modifier = Modifier.offset(x = 0.dp, y = 150.dp),
            contentAlignment = Alignment.Center
        ) {
            SelectLanguageButton(true, viewModel)
        }

        Button(
            onClick = { viewModel.translate() },
            modifier = Modifier.offset(x = 0.dp, y = 105.dp)
        ) {
            Text("Translate")
        }
    }

}

