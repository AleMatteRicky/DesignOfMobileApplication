package com.example.augmentedrealityglasses.translation

import kotlinx.coroutines.flow.StateFlow

interface TranslationViewModelContract {
    val uiState: StateFlow<TranslationUiState>

    fun downloadLanguageModel(languageTag: String)

    fun selectTargetLanguage(languageTag: String?)

    fun selectSourceLanguage(languageTag: String?)
}