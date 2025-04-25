package com.example.augmentedrealityglasses.translation

data class TranslationUiState(
    val sourceLanguage: String? = null,
    val targetLanguage: String? = null,
    val recognizeLanguage: Boolean = true,
    val isRecording: Boolean = false,
    val recognizedText: String = "",
    val translatedText: String = ""
)

