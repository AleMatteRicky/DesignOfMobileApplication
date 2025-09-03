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
import com.example.augmentedrealityglasses.BluetoothUpdateStatus
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

class TranslationViewModel(
    private val systemLanguage: String,
    private val application: Application,
    private val bleManager: RemoteDeviceManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TranslationUiState())

    val uiState = _uiState.asStateFlow()

    var recorder: SpeechRecognizer? = null

    var recordJob: Job? =
        null //used to keep track of started recordJob in order to cancel them if the user starts a new recordJob before the old one finishes

    var translator: Translator? = null

    var translatorJob: Job? = null

    val modelManager: RemoteModelManager = RemoteModelManager.getInstance()

    var isSourceModelNotAvailable = false

    var isTargetModelNotAvailable = false

    var isExtDeviceConnected by mutableStateOf(false)
        private set

    val internetConnectionManager: InternetConnectionManager =
        InternetConnectionManager(application)

    var bluetoothUpdateStatus by mutableStateOf(BluetoothUpdateStatus.NONE)
        private set

    var errorMessage = MutableStateFlow("")

    private val TAG: String = "TranslationViewModel"

    init {
        viewModelScope.launch {
            if (bleManager.isDeviceSet()) {
                bleManager.receiveUpdates()
                    .collect { connectionState ->
                        if (connectionState.connectionState is ConnectionState.Connected) {
                            isExtDeviceConnected = true
                            bluetoothUpdateStatus = BluetoothUpdateStatus.DEVICE_CONNECTED
                        } else {
                            isExtDeviceConnected = false
                            bluetoothUpdateStatus = BluetoothUpdateStatus.DEVICE_DISCONNECTED
                        }
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

        if (!_uiState.value.sourceLanguage.isNullOrBlank() && internetConnectionManager.status == ConnectivityStatus.ValidatedInternet) {
            initializeSpeechRecognizer()
            _uiState.update { _uiState.value.copy(isRecording = true, recognizedText = "") }
            recordJob = viewModelScope.launch {
                recorder?.startListening(createIntent())
            }
        }
    }

    //todo add exception handling for all the synchronous methods

    fun stopRecording() {
        recorder?.stopListening()
        recorder?.destroy()
        recordJob?.cancel()
        _uiState.update { _uiState.value.copy(isRecording = false) }
        if (!_uiState.value.isResultReady && _uiState.value.recognizedText.isNotEmpty()) {
            _uiState.update { _uiState.value.copy(isResultReady = true) }
        }
    }

    fun selectTargetLanguage(targetLanguage: String?) {
        _uiState.update {
            _uiState.value.copy(
                targetLanguage = targetLanguage,
            )
        }
        if (_uiState.value.recognizedText.isNotEmpty()) {
            if (_uiState.value.targetLanguage.isNullOrBlank()) {
                _uiState.update {
                    _uiState.value.copy(
                        translatedText = "",
                    )
                }
            } else {
                translate()
            }
        }
    }

    fun selectSourceLanguage(sourceLanguage: String?) {
        _uiState.update {
            _uiState.value.copy(
                sourceLanguage = sourceLanguage,
            )
        }
    }

    fun resetResultStatus() {
        _uiState.update { _uiState.value.copy(isResultReady = false) }
    }

    fun clearText() {
        _uiState.update { _uiState.value.copy(recognizedText = "", translatedText = "") }
    }

    fun translate() {

        translatorJob?.cancel()

        if (_uiState.value.targetLanguage != null) {
            translatorJob = viewModelScope.launch {
                if (_uiState.value.targetLanguage != null && _uiState.value.sourceLanguage != null) {
                    checkModelDownloaded()
                    if (_uiState.value.isModelNotAvailable) {
                        downloadSourceAndTargetLanguageModel()
                    } else { //not executed, translation called by downloadSourceAndTargetLanguage after the download is finished
                        initializeTranslator()
                        translator?.translate(_uiState.value.recognizedText)
                            ?.addOnSuccessListener { translatedText ->
                                Log.d("Translation succeeded", translatedText)
                                _uiState.update {
                                    _uiState.value.copy(translatedText = translatedText)
                                }
                                //send translated text to esp32
                                Log.d(TAG, translatedText)
                                sendBluetoothMessage(_uiState.value.translatedText)
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
            .setSourceLanguage(_uiState.value.sourceLanguage!!)
            .setTargetLanguage(_uiState.value.targetLanguage!!)
            .build()

        translator = Translation.getClient(options)
    }

    private suspend fun checkModelDownloaded() {
        val targetLanguageRemoteModel: RemoteModel =
            TranslateRemoteModel.Builder(_uiState.value.targetLanguage!!).build()
        val sourceLanguageRemoteModel: RemoteModel =
            TranslateRemoteModel.Builder(_uiState.value.sourceLanguage!!).build()
        isSourceModelNotAvailable =
            !modelManager.isModelDownloaded(sourceLanguageRemoteModel).await()
        isTargetModelNotAvailable =
            !modelManager.isModelDownloaded(targetLanguageRemoteModel).await()

        _uiState.update {
            _uiState.value.copy(isModelNotAvailable = isSourceModelNotAvailable || isTargetModelNotAvailable)
        }
    }

    private fun initializeDownloadedLanguages() {
        viewModelScope.launch {

            val (downloaded, notDownloaded) = TranslateLanguage.getAllLanguages()
                .sortedBy { tag -> getFullLengthName(tag) }.partition { tag ->
                    modelManager.isModelDownloaded(
                        TranslateRemoteModel.Builder(tag).build()
                    ).await()
                }

            _uiState.update {
                _uiState.value.copy(
                    downloadedLanguageTags = MutableStateFlow(downloaded),
                    notDownloadedLanguageTags = MutableStateFlow(notDownloaded)
                )
            }
        }
    }

    private fun downloadSourceAndTargetLanguageModel() {

        if (isSourceModelNotAvailable && !_uiState.value.isDownloadingSourceLanguageModel) {
            viewModelScope.launch {
                _uiState.update { _uiState.value.copy(isDownloadingSourceLanguageModel = true) }
                downloadSourceOrTargetLanguageModel(isSource = true)
                _uiState.update { _uiState.value.copy(isDownloadingSourceLanguageModel = false) }
                if (!isTargetModelNotAvailable) {
                    translate()
                }
            }
        }
        if (isTargetModelNotAvailable && !_uiState.value.isDownloadingTargetLanguageModel) { //in practice should never happen, a target language can only be selected if it is available
            viewModelScope.launch {
                _uiState.update { _uiState.value.copy(isDownloadingTargetLanguageModel = true) }
                downloadSourceOrTargetLanguageModel(isSource = false)
                _uiState.update { _uiState.value.copy(isDownloadingTargetLanguageModel = false) }
                if (!isSourceModelNotAvailable) {
                    translate()
                }
            }
        }
        _uiState.update {
            _uiState.value.copy(isModelNotAvailable = isTargetModelNotAvailable || isSourceModelNotAvailable)
        }

    }

    fun downloadLanguageModel(languageTag: String) {
        viewModelScope.launch {
            val currentlyDownloadingLanguageTags = _uiState.value.currentlyDownloadingLanguageTags
            currentlyDownloadingLanguageTags.update { currentlyDownloadingLanguageTags.value + languageTag }
            try {
                modelManager.download(
                    TranslateRemoteModel.Builder(languageTag).build(),
                    DownloadConditions.Builder().build()
                ).await()
                val downloadedLanguageTags = _uiState.value.downloadedLanguageTags
                val notDownloadedLanguageTags = _uiState.value.notDownloadedLanguageTags
                notDownloadedLanguageTags.update {
                    notDownloadedLanguageTags.value - languageTag
                }
                downloadedLanguageTags.update {
                    val elementIndex =
                        downloadedLanguageTags.value.map { tag -> getFullLengthName(tag) }
                            .binarySearch(getFullLengthName(languageTag))
                    val insertionIndex =
                        if (elementIndex >= 0) elementIndex else -(elementIndex + 1)
                    downloadedLanguageTags.value.take(insertionIndex) + languageTag + downloadedLanguageTags.value.drop(
                        insertionIndex
                    )
                }
            } catch (_: Exception) {
                Log.e("Download Failed", "Failure")
                errorMessage.value = "Download failed"
            } finally {
                currentlyDownloadingLanguageTags.update { currentlyDownloadingLanguageTags.value - languageTag }
            }
        }
    }

    private suspend fun downloadSourceOrTargetLanguageModel(isSource: Boolean) {
        try {
            modelManager.download(
                TranslateRemoteModel.Builder(if (isSource) _uiState.value.sourceLanguage!! else _uiState.value.targetLanguage!!)
                    .build(),
                DownloadConditions.Builder().build()
            ).await()
            if (isSource) {
                isSourceModelNotAvailable = false
            } else {
                isTargetModelNotAvailable = false
            }
        } catch (e: Exception) {
            errorMessage.value = "Download failed"
            Log.e("Download Failed", "Failure")
        }
    }

    fun setSelectingLanguageRole(languageRole: LanguageRole?) { //needed to communicate the selecting type of language to the translation language selection screen
        _uiState.update { _uiState.value.copy(selectingLanguageRole = languageRole) }
    }

    /*
    private suspend fun identifySourceLanguage(): Boolean { //True if the identification is successful, False otherwise
        val languageIdentification = LanguageIdentification.getClient()
        val tag = languageIdentification.identifyLanguage(_uiState.value.recognizedText).await()
        if (tag != "und") {
            //uiState = uiState.copy(sourceLanguage = TranslateLanguage.fromLanguageTag(tag))
            return true
        } else {
            if (_uiState.value.sourceLanguage != null) {
                //uiState = uiState.copy(sourceLanguage = null) //todo add recommended language
            }
            Log.e("Undefined Language", "Exception")
            return false
        }
    }
     */

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun initializeSpeechRecognizer() {
        recorder = createSpeechRecognizer(application)

        recorder?.setRecognitionListener(createRecognitionListener())

    }

    //the recognizer could still recognize other languages non only the preferred one

    private fun createIntent(): Intent {

        val sourceLanguageTag =
            adaptLanguageTag(_uiState.value.sourceLanguage!!) //already checked in the invocation

        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, sourceLanguageTag)
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                sourceLanguageTag
            )
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

    //Speech recognizer require a format "aa-AA" where aa is the languageTag and AA is the regional version,
    // if the speech recognizer does not recognize the regional language AA it proceeds with the default one
    private fun adaptLanguageTag(languageTag: String): String { //from short BCP-47 to BCP-47 with regional variant ex: from "it" to "it-IT"

        return languageTag + "-" + languageTag.uppercase()
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {

            var isListening = true

            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SpeechRecognition", "Ready for speech. Parameters: $params")
            }

            override fun onBeginningOfSpeech() {
            }

            override fun onRmsChanged(rmsDbValue: Float) {
                Log.d("SpeechRecognition", "Rms changed. Parameters: $rmsDbValue")

                _uiState.update {
                    _uiState.value.copy(currentNormalizedRms = normalizeRms(rmsDbValue)) //normalized value are used to limit the animation behaviour
                }
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
                Log.e("SpeechRecognition Error", "Error: $errorMessage (Code: $error)")
            }

            override fun onResults(results: Bundle?) {

                isListening = false
                val data: ArrayList<String>? =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val newRecognizedText = data.toString().removePrefix("[").removeSuffix("]")

                if(newRecognizedText != "") {
                    _uiState.update {
                        _uiState.value.copy(
                            recognizedText = newRecognizedText,
                        )
                    }
                    viewModelScope.launch {
                        if (_uiState.value.targetLanguage != null) {
                            translate()
                        } else {
                            Log.d("send final result", _uiState.value.recognizedText)
                            sendBluetoothMessage(_uiState.value.recognizedText)
                        }
                    }
                    if (_uiState.value.recognizedText.isNotEmpty()) {
                        _uiState.update { _uiState.value.copy(isResultReady = true) }
                    }
                }

                stopRecording()
                Log.d("SpeechRecognizer", "Speech recognition results received: $data")
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val data: ArrayList<String>? =
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val newRecognizedText = data.toString().removePrefix("[").removeSuffix("]")

                Log.d("Speech Recognition Partial", "Speech recognition partial results received: $newRecognizedText")

                if(newRecognizedText != "" && isListening) {
                    _uiState.update {
                        _uiState.value.copy(
                            recognizedText = newRecognizedText
                        )
                    }
                    if (_uiState.value.recognizedText != "") {
                        viewModelScope.launch {
                            if (_uiState.value.targetLanguage != null) {
                                translate()
                            } else {
                                Log.d("send", _uiState.value.recognizedText)
                                sendBluetoothMessage(_uiState.value.recognizedText) //the method checks if the device is connected before sending anything
                            }
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

        if (isExtDeviceConnected) {
            viewModelScope.launch {
                bleManager.send(msg)
            }
        } else {
            Log.d(TAG, "External device not connected")
        }
    }

    fun hideErrorMessage() {
        errorMessage.value = ""
    }

    fun hideBluetoothUpdate() {
        bluetoothUpdateStatus = BluetoothUpdateStatus.NONE
    }

}