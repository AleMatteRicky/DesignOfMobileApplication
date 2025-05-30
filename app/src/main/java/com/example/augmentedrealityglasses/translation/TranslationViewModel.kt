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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.augmentedrealityglasses.App
import com.example.augmentedrealityglasses.ble.device.RemoteDeviceManager
import com.example.augmentedrealityglasses.ble.viewmodels.ConnectViewModel
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModel
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TranslationViewModel(
    private val systemLanguage: String,
    private val application: Application,
    private val bleManager: RemoteDeviceManager
) : AndroidViewModel(application) {
    var uiState by mutableStateOf(TranslationUiState())
        private set

    var recorder: SpeechRecognizer? = null

    var recordJob: Job? =
        null //used to keep track of started recordJob in order to cancel them if the user starts a new recordJob before the old one finishes

    var translator: Translator? = null

    var translatorJob: Job? = null

    val modelManager: RemoteModelManager = RemoteModelManager.getInstance()

    var isSourceModelNotAvailable = false

    var isTargetModelNotAvailable = false

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as App
                val bleManager = application.container.bleManager
                TranslationViewModel (
                    systemLanguage = TranslateLanguage.ITALIAN,
                    application = application,
                    bleManager = bleManager
                )
            }
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording() {
        recordJob?.cancel()
        initializeSpeechRecognizer()
        uiState = uiState.copy(isRecording = true, recognizedText = "")
        recordJob = viewModelScope.launch {
            recorder?.startListening(createIntent())
        }
    }

    //todo add exception handling for all the synchronous methods

    fun stopRecording() { //todo the recorder stops automatically after few seconds during which it does not receive audio input, change it
        recorder?.stopListening()
        recorder?.destroy()
        recordJob?.cancel()
        uiState = uiState.copy(isRecording = false)
    }

    fun selectTargetLanguage(targetLanguage: String?) {
        uiState = uiState.copy(targetLanguage = targetLanguage, isModelNotAvailable = false) //isModelNotAvailable is set to false in order to force recomposition if the new target language need to be installed
    }

    fun translate() {

        translatorJob?.cancel()

        if(uiState.targetLanguage != null) {
            translatorJob = viewModelScope.launch {
                if (uiState.targetLanguage != null) {
                    if(identifySourceLanguage()) {
                        checkModelDownloaded()
                        if (!uiState.isModelNotAvailable) {
                            initializeTranslator()
                            translator?.translate(uiState.recognizedText)
                                ?.addOnSuccessListener { translatedText ->
                                    Log.d("Translation succeeded", translatedText)
                                    uiState = uiState.copy(translatedText = translatedText)
                                    //send translated text to esp32
                                    Log.d("send ",translatedText)
                                    bleManager.send(uiState.translatedText)
                                }
                                ?.addOnFailureListener { exception ->
                                    Log.e("Translation failed", exception.toString())
                                }
                        }
                    }
                }
            }
        }
    }

    fun initializeTranslator() {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(uiState.sourceLanguage!!)
            .setTargetLanguage(uiState.targetLanguage!!)
            .build()

        translator = Translation.getClient(options)
    }

    private suspend fun checkModelDownloaded() {
        val targetLanguageRemoteModel: RemoteModel =
            TranslateRemoteModel.Builder(uiState.targetLanguage!!).build()
        val sourceLanguageRemoteModel: RemoteModel =
            TranslateRemoteModel.Builder(uiState.sourceLanguage!!).build()
        isSourceModelNotAvailable =
            !modelManager.isModelDownloaded(sourceLanguageRemoteModel).await()
        isTargetModelNotAvailable =
            !modelManager.isModelDownloaded(targetLanguageRemoteModel).await()

        uiState =
            uiState.copy(isModelNotAvailable = isSourceModelNotAvailable || isTargetModelNotAvailable)
    }

    fun downloadLanguageModel() {
        viewModelScope.launch {
            uiState = uiState.copy(isDownloadingLanguageModel = true)
            if (isSourceModelNotAvailable) {
                downloadSourceLanguageModel()
            }
            if (isTargetModelNotAvailable) {
                downloadTargetLanguageModel()
            }
            uiState =
                uiState.copy(isModelNotAvailable = isTargetModelNotAvailable || isSourceModelNotAvailable)
            uiState = uiState.copy(isDownloadingLanguageModel = false)
        }

    }

    private suspend fun downloadSourceLanguageModel() {
        modelManager.download(
            TranslateRemoteModel.Builder(uiState.sourceLanguage!!).build(),
            DownloadConditions.Builder().build()
        ).await()
        isSourceModelNotAvailable = false
    }

    private suspend fun downloadTargetLanguageModel() {
        modelManager.download(
            TranslateRemoteModel.Builder(uiState.targetLanguage!!).build(),
            DownloadConditions.Builder().build()
        ).await()
        isTargetModelNotAvailable = false
    }

    private suspend fun identifySourceLanguage() : Boolean { //True if the identification is successful, False otherwise
        val languageIdentification = LanguageIdentification.getClient()
        val tag = languageIdentification.identifyLanguage(uiState.recognizedText).await()
        if (tag != "und") {
            uiState = uiState.copy(sourceLanguage = TranslateLanguage.fromLanguageTag(tag))
            //todo tag could be not supported by mlkit translate
            //todo
            return true
        } else {
            Log.e("Undefined Language", "Exception")
            return false
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun initializeSpeechRecognizer() {
        recorder = createSpeechRecognizer(application)

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
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                60000L
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                60000L
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

            override fun onBufferReceived(value: ByteArray?) {
            }

            override fun onEndOfSpeech() {
                //uiState = uiState.copy(isRecording = false)
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
                    recognizedText = data.toString().removePrefix("[").removeSuffix("]"),
                )
                if (uiState.targetLanguage != null) {
                    translate()
                }
                Log.d("SpeechRecognizer", "Speech recognition results received: $data")
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val data: ArrayList<String>? =
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                uiState = uiState.copy(
                    recognizedText = data.toString().removePrefix("[").removeSuffix("]")
                )
                Log.d("SpeechRecognizer", "Speech recognition partial results received: $data")

                if(uiState.recognizedText != "") {
                    if (uiState.targetLanguage != null) {
                        translate()
                    }
                    else{
                        Log.d("send", uiState.recognizedText)
                        bleManager.send(uiState.recognizedText)
                    }
                }
            }

            override fun onEvent(x: Int, y: Bundle?) {
            }
        }
    }
}