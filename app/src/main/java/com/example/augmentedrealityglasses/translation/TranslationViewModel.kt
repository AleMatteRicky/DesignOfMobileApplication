package com.example.augmentedrealityglasses.translation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.Job

class TranslationViewModel(
    private val systemLanguage: TranslateLanguage,
) : ViewModel() {
    var uiState by mutableStateOf(TranslationUiState())
        private set

    var recordJob: Job? =
        null //used to keep track of started recordJob in order to cancel them if the user starts a new recordJob before the old one finishes

    fun record() {}

}