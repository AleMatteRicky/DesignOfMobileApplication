package com.example.augmentedrealityglasses.translation

import com.google.mlkit.nl.translate.TranslateLanguage

data class TranslationUiState(
    val fromLanguage: TranslateLanguage? = null,
    val toLanguage: TranslateLanguage? = null,
    val recognizeLanguage: Boolean = true
)

