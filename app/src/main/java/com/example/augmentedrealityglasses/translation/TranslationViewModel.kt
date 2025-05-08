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
    private val systemLanguage: String, application: Application
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


    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording() {
        recordJob?.cancel()
        initializeSpeechRecognizer()
        uiState = uiState.copy(isRecording = true, recognizedText = "")
        recordJob = viewModelScope.launch {
            recorder?.startListening(createIntent())
        }
    }

    fun stopRecording() { //todo the recorder stops automatically after few seconds during which it does not receive audio input, change it
        recorder?.stopListening()
        recorder?.destroy()
        recordJob?.cancel()
        uiState = uiState.copy(isRecording = false)
    }

    fun selectTargetLanguage(targetLanguage: String) {
        uiState = uiState.copy(targetLanguage = targetLanguage)
    }

    fun translate() {

        if (uiState.isRecording) {
            stopRecording()
        }

        translatorJob?.cancel()

        translatorJob = viewModelScope.launch {
            if (uiState.targetLanguage != null) {
                identifySourceLanguage() //todo check if could ever happen that the initialization do not wait for the identification
                checkModelDownloaded()
                if (!uiState.isModelNotAvailable) {
                    initializeTranslator()
                    translator?.translate(uiState.recognizedText)
                        ?.addOnSuccessListener { translatedText ->
                            Log.d("Translation succeeded", translatedText)
                            uiState = uiState.copy(translatedText = translatedText)
                        }
                        ?.addOnFailureListener { exception ->
                            Log.e("Translation failed", exception.toString())
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

    //todo add a button to download the language model if it is not already downloaded on the device
    fun downloadLanguageModel() {
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

    private fun downloadSourceLanguageModel() {
        modelManager.download(
            TranslateRemoteModel.Builder(uiState.sourceLanguage!!).build(),
            DownloadConditions.Builder().build()
        )
            .addOnSuccessListener {
                Log.d("Correct", "Download of source language succeeded")
                isSourceModelNotAvailable = false
            }
            .addOnFailureListener {
                Log.d("Error", "Download of source language failed")
                //todo add error handling
            }
    }

    private fun downloadTargetLanguageModel() {
        modelManager.download(
            TranslateRemoteModel.Builder(uiState.targetLanguage!!).build(),
            DownloadConditions.Builder().build()
        )
            .addOnSuccessListener {
                Log.d("Correct", "Download of target language succeeded")
                isTargetModelNotAvailable = false
            }
            .addOnFailureListener {
                Log.d("Error", "Download of target language failed")
                //todo add error handling
            }
    }

    //todo try to remove nested Tasks

    private suspend fun identifySourceLanguage() {
        val languageIdentification = LanguageIdentification.getClient()
        val tag = languageIdentification.identifyLanguage(uiState.recognizedText).await()
        if (tag != "und") {
            uiState = uiState.copy(sourceLanguage = TranslateLanguage.fromLanguageTag(tag))
            //todo tag could be not supported by mlkit translate
            //todo
        } else {
            Log.e("Undefined Language", "Exception")
        }
    }

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