package com.example.augmentedrealityglasses.translation

import android.Manifest
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.SpeechRecognizer.createSpeechRecognizer
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class TranslationViewModel(
    private val systemLanguage: String, application: Application,
) : AndroidViewModel(application) {
    var uiState by mutableStateOf(TranslationUiState())
        private set

    var recorder: SpeechRecognizer? = null

    var recordJob: Job? =
        null //used to keep track of started recordJob in order to cancel them if the user starts a new recordJob before the old one finishes

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun initializeSpeechRecognizer() {
        recorder = createSpeechRecognizer(getApplication())

        recorder?.setRecognitionListener(createRecognitionListener())


    }

    private fun createIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS, true
            )
        }

    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SpeechRecognition", "Ready for speech. Parameters: $params")
            }

            override fun onBeginningOfSpeech() {
            }

            override fun onRmsChanged(params: Float) {
                Log.d("SpeechRecognition", "Rms changed. Parameters: $params")
            }

            override fun onBufferReceived(p0: ByteArray?) {
            }

            override fun onEndOfSpeech() {
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown speech recognition error"
                }
                Log.e("SpeechRecognition", "Error: $errorMessage (Code: $error)")
            }

            override fun onResults(results: Bundle?) {
                val data: ArrayList<String>? =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                uiState = uiState.copy(
                    recognizedText = data.toString().removePrefix("[").removeSuffix("]")
                )
                Log.d("SpeechRecognizer", "Speech recognition results received: $data")
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val data: ArrayList<String>? =
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                uiState = uiState.copy(
                    recognizedText = data.toString().removePrefix("[").removeSuffix("]")
                )
                Log.d("SpeechRecognizer", "Speech recognition partial results received: $data")
            }

            override fun onEvent(p0: Int, p1: Bundle?) {
            }
        }
    }
}