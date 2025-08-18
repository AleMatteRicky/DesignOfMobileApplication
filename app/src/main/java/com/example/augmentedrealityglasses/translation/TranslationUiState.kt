package com.example.augmentedrealityglasses.translation

import kotlinx.coroutines.flow.MutableStateFlow

data class TranslationUiState(
    val sourceLanguage: String? = null,
    val targetLanguage: String? = null,
    val recognizeLanguage: Boolean = true,
    val isRecording: Boolean = false,
    val recognizedText: String = "",
    val translatedText: String = "",
    val isDownloadingSourceLanguageModel: Boolean = false,
    val isDownloadingTargetLanguageModel: Boolean = false,
    val downloadedLanguageTags: List<String> = emptyList(),
    val notDownloadedLanguageTags: List<String> = emptyList(),
    val isModelNotAvailable: Boolean = false,
    val currentNormalizedRms: Float = 0f,
    val currentlyDownloadingLanguageTags: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet()),
    val isResultReady: Boolean = false
)

