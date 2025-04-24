package com.example.augmentedrealityglasses.translation

data class TranslationUiState(
    val fromLanguage: String? = null,
    val toLanguage: String? = null,
    val recognizeLanguage: Boolean = true,
    val isRecording: Boolean = false,
    val recognizedText: String = "",
    val translatedText: String = ""
)

