package com.example.augmentedrealityglasses.translation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeTranslationViewModel(
    initialState: TranslationUiState = TranslationUiState()
) : TranslationViewModelContract {

    private val _uiState = MutableStateFlow(initialState)
    override val uiState: StateFlow<TranslationUiState> = _uiState

    val downloadedCalls = mutableListOf<String>()
    val selectedTargetCalls = mutableListOf<String?>()
    val selectedSourceCalls = mutableListOf<String?>()

    override fun downloadLanguageModel(languageTag: String) {
        downloadedCalls.add(languageTag)
        val current = _uiState.value
        _uiState.value = current.copy(
            currentlyDownloadingLanguageTags = MutableStateFlow(current.currentlyDownloadingLanguageTags.value + languageTag)
        )
    }

    override fun selectTargetLanguage(languageTag: String?) {
        selectedTargetCalls.add(languageTag)
        _uiState.value = _uiState.value.copy(targetLanguage = languageTag)
    }

    override fun selectSourceLanguage(languageTag: String?) {
        selectedSourceCalls.add(languageTag)
        _uiState.value = _uiState.value.copy(sourceLanguage = languageTag)
    }
}