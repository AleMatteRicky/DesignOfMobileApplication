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
import com.example.augmentedrealityglasses.ble.devicedata.RemoteDeviceManager
import com.example.augmentedrealityglasses.ble.peripheral.gattevent.ConnectionState
import com.example.augmentedrealityglasses.internet.ConnectivityStatus
import com.example.augmentedrealityglasses.internet.InternetConnectionManager
import com.example.augmentedrealityglasses.translation.ui.LanguageRole
import com.example.augmentedrealityglasses.translation.ui.getFullLengthName
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

//todo fix when offline multiple lines are repeated, offline recorder adds more partial results
//todo fix behaviour when language is undefined
//todo languages are not always sorted by alphabetic order

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

    val internetConnectionManager: InternetConnectionManager =
        InternetConnectionManager(application)

    var errorMessage by mutableStateOf("")

    private val TAG: String = "TranslationViewModel"

    init {
        viewModelScope.launch {
            if (bleManager.isDeviceSet()) {
                bleManager.receiveUpdates()
                    .collect { connectionState ->
                        uiState =
                            uiState.copy(isExtDeviceConnected = connectionState.connectionState is ConnectionState.Connected)
                    }
            }
        }
        initializeDownloadedLanguages()
        internetConnectionManager.start()

    }

    override fun onCleared() {
        super.onCleared()
        internetConnectionManager.stop()
    }

    //TODO fix when mic is open but no audio is recorded

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as App
                val bleManager = application.container.proxy
                TranslationViewModel(
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

        if (!uiState.sourceLanguage.isNullOrBlank() && internetConnectionManager.status == ConnectivityStatus.ValidatedInternet) {
            initializeSpeechRecognizer()
            uiState = uiState.copy(isRecording = true, recognizedText = "")
            recordJob = viewModelScope.launch {
                recorder?.startListening(createIntent())
            }
        }
    }

    //todo add exception handling for all the synchronous methods

    fun stopRecording() { //todo the recorder stops automatically after few seconds during which it does not receive audio input, change it
        recorder?.stopListening()
        recorder?.destroy()
        recordJob?.cancel()
        uiState = uiState.copy(isRecording = false)
        if (!uiState.isResultReady && uiState.recognizedText.isNotEmpty()) {
            uiState = uiState.copy(isResultReady = true)
        }
    }

    fun selectTargetLanguage(targetLanguage: String?) {
        uiState = uiState.copy(
            targetLanguage = targetLanguage,
        )
        if (uiState.recognizedText.isNotEmpty()) {
            translate() //todo check if it could be useful to use a coroutine
        }
    }

    fun selectSourceLanguage(sourceLanguage: String?) {
        uiState = uiState.copy(
            sourceLanguage = sourceLanguage,
        )
    }

    fun resetResultStatus() {
        uiState = uiState.copy(isResultReady = false)
    }

    fun clearText() {
        uiState = uiState.copy(recognizedText = "", translatedText = "")
    }

    fun translate() {

        translatorJob?.cancel()

        if (uiState.targetLanguage != null) {
            translatorJob = viewModelScope.launch {
                if (uiState.targetLanguage != null && uiState.sourceLanguage != null) {
                    checkModelDownloaded()
                    if (uiState.isModelNotAvailable) {
                        downloadSourceAndTargetLanguageModel()
                    } else { //not executed, translation called by downloadSourceAndTargetLanguage after the download is finished
                        initializeTranslator()
                        translator?.translate(uiState.recognizedText)
                            ?.addOnSuccessListener { translatedText ->
                                Log.d("Translation succeeded", translatedText)
                                uiState = uiState.copy(translatedText = translatedText)
                                //send translated text to esp32
                                Log.d(TAG, translatedText)
                                sendBluetoothMessage(uiState.translatedText)
                            }
                            ?.addOnFailureListener { exception ->
                                Log.e("Translation failed", exception.toString())
                            }
                    }


                }
            }
        }
    }

    private fun initializeTranslator() {
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

    private fun initializeDownloadedLanguages() {
        viewModelScope.launch {

            val (downloaded, notDownloaded) = TranslateLanguage.getAllLanguages()
                .sortedBy { tag -> getFullLengthName(tag) }.partition { tag ->
                    modelManager.isModelDownloaded(
                        TranslateRemoteModel.Builder(tag).build()
                    ).await()
                }

            uiState = uiState.copy(
                downloadedLanguageTags = downloaded, notDownloadedLanguageTags = notDownloaded
            )
        }
    }

    /*
    todo improve the behaviour, when the user tries to translate for the first time,
        the viewmodel needs to download the source language. if the download is interrupted the viewmodel
        will start another one
     */

    private fun downloadSourceAndTargetLanguageModel() {

        if (isSourceModelNotAvailable && !uiState.isDownloadingSourceLanguageModel) {
            viewModelScope.launch {
                uiState = uiState.copy(isDownloadingSourceLanguageModel = true)
                downloadSourceOrTargetLanguageModel(isSource = true)
                uiState = uiState.copy(isDownloadingSourceLanguageModel = false)
                if (!isTargetModelNotAvailable) {
                    translate()
                }
            }
        }
        if (isTargetModelNotAvailable && !uiState.isDownloadingTargetLanguageModel) { //in practice should never happen, a target language can only be selected if it is available
            viewModelScope.launch {
                uiState = uiState.copy(isDownloadingTargetLanguageModel = true)
                downloadSourceOrTargetLanguageModel(isSource = false)
                uiState = uiState.copy(isDownloadingTargetLanguageModel = false)
                if (!isSourceModelNotAvailable) {
                    translate()
                }
            }
        }
        uiState =
            uiState.copy(isModelNotAvailable = isTargetModelNotAvailable || isSourceModelNotAvailable)

    }

    fun downloadLanguageModel(languageTag: String) {
        viewModelScope.launch {
            val currentlyDownloadedLanguageTags = uiState.currentlyDownloadingLanguageTags
            currentlyDownloadedLanguageTags.update { currentlyDownloadedLanguageTags.value + languageTag }
            try {
                modelManager.download(
                    TranslateRemoteModel.Builder(languageTag).build(),
                    DownloadConditions.Builder().build()
                ).await()
                uiState = uiState.copy(
                    downloadedLanguageTags = uiState.downloadedLanguageTags + languageTag,
                    notDownloadedLanguageTags = uiState.notDownloadedLanguageTags - languageTag
                )
            } catch (e: Exception) {
                Log.e("Download Failed", "Failure") //todo add message in app
            } finally {
                currentlyDownloadedLanguageTags.update { currentlyDownloadedLanguageTags.value - languageTag }
            }
        }
    }

    private suspend fun downloadSourceOrTargetLanguageModel(isSource: Boolean) {
        try {
            modelManager.download(
                TranslateRemoteModel.Builder(if (isSource) uiState.sourceLanguage!! else uiState.targetLanguage!!)
                    .build(),
                DownloadConditions.Builder().build()
            ).await()
            if (isSource) {
                isSourceModelNotAvailable = false
            } else {
                isTargetModelNotAvailable = false
            }
        } catch (e: Exception) {
            Log.e("Download Failed", "Failure") //todo add message in app
        }
    }

    fun setSelectingLanguageRole(languageRole: LanguageRole?) { //needed to communicate the selecting type of language to the translation language selection screen
        uiState = uiState.copy(selectingLanguageRole = languageRole)
    }

    private suspend fun identifySourceLanguage(): Boolean { //True if the identification is successful, False otherwise
        val languageIdentification = LanguageIdentification.getClient()
        val tag = languageIdentification.identifyLanguage(uiState.recognizedText).await()
        if (tag != "und") {
            //uiState = uiState.copy(sourceLanguage = TranslateLanguage.fromLanguageTag(tag))
            //todo tag could be not supported by mlkit translate
            //todo
            return true
        } else {
            if (uiState.sourceLanguage != null) {
                //uiState = uiState.copy(sourceLanguage = null) //todo add recommended language
            }
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

        val sourceLanguageTag = adaptLanguageTag(uiState.sourceLanguage!!)//todo add exception

        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, sourceLanguageTag)
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                sourceLanguageTag
            ) //can recognize other languages
            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS, true
            )
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
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

    private fun adaptLanguageTag(languageTag: String): String { //ex: from "it" to "it-IT"
        return languageTag + "-" + languageTag.uppercase()
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SpeechRecognition", "Ready for speech. Parameters: $params")
            }

            override fun onBeginningOfSpeech() {
            }

            override fun onRmsChanged(rmsDbValue: Float) {
                Log.d("SpeechRecognition", "Rms changed. Parameters: $rmsDbValue")

                uiState =
                    uiState.copy(currentNormalizedRms = normalizeRms(rmsDbValue)) //normalized value are used to limit the animation behaviour

            }

            override fun onBufferReceived(value: ByteArray?) {
            }

            //this function is executed before onResult so stopping here the listening would lead to never executing onResult
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
                stopRecording()
                Log.e("SpeechRecognition", "Error: $errorMessage (Code: $error)")
            }

            override fun onResults(results: Bundle?) {
                val data: ArrayList<String>? =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                uiState = uiState.copy(
                    recognizedText = data.toString().removePrefix("[").removeSuffix("]"),
                )
                viewModelScope.launch {
                    //identifySourceLanguage()
                    if (uiState.targetLanguage != null) {
                        translate()
                    } else {
                        Log.d("send final result", uiState.recognizedText)
                        sendBluetoothMessage(uiState.recognizedText)
                    }
                }
                if (uiState.recognizedText.isNotEmpty()) {
                    uiState = uiState.copy(isResultReady = true)
                }
                stopRecording()
                Log.d("SpeechRecognizer", "Speech recognition results received: $data")
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val data: ArrayList<String>? =
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                uiState = uiState.copy(
                    recognizedText = data.toString().removePrefix("[").removeSuffix("]")
                )
                Log.d(TAG, "Speech recognition partial results received: $data")

                if (uiState.recognizedText != "") {
                    viewModelScope.launch {
                        //identifySourceLanguage()
                        if (uiState.targetLanguage != null) {
                            translate()
                        } else {
                            Log.d("send", uiState.recognizedText)
                            sendBluetoothMessage(uiState.recognizedText) //the method checks if the device is connected before sending anything
                        }
                    }

                }
            }

            override fun onEvent(x: Int, y: Bundle?) {
            }
        }
    }

    private fun normalizeRms(rmsValueDb: Float): Float {
        val rmsClipped = rmsValueDb.coerceIn(
            -2f,
            10f
        ) //not used in practice, empirically a speechRecognizer seems to record rms value between -2db and 10db
        return (rmsClipped + 2f) / 12f //max value 10db, min value -2db, linear normalization
    }

    private fun sendBluetoothMessage(
        msgContent: String
    ) {

        //Main json object that is sent through ble connection
        val jsonToSend = JSONObject()

        jsonToSend.put("command", "t")
        jsonToSend.put("text", msgContent)

        val msg = jsonToSend.toString()

        Log.d(TAG, "BLE message:\n$msg")

        if (uiState.isExtDeviceConnected) {
            viewModelScope.launch {
                bleManager.send(msg)
            }
        } else {
            Log.d(TAG, "External device not connected")
        }
    }

    fun hideErrorMessage() {
        errorMessage = ""
    }

}