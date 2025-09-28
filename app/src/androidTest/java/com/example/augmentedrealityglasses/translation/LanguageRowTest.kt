package com.example.augmentedrealityglasses.translation

import com.example.augmentedrealityglasses.translation.ui.LanguageRole
import com.example.augmentedrealityglasses.translation.ui.LanguageRow
import com.example.augmentedrealityglasses.translation.ui.getFullLengthName

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.augmentedrealityglasses.translation.TranslationUiState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test


class LanguageRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun rowTag(tag: String?) = "LANG_ROW_${tag ?: "null"}"
    private fun downloadTag(tag: String?) = "DOWNLOAD_BTN_${tag ?: "null"}"
    private fun progressTag(tag: String?) = "PROGRESS_${tag ?: "null"}"

    @Test
    fun rendersLanguageName() {
        val tag = "it"
        val fake = FakeTranslationViewModel(
            initialState = TranslationUiState(
                currentlyDownloadingLanguageTags = MutableStateFlow(emptySet())
            )
        )

        composeRule.setContent {
            LanguageRow(
                modifier = Modifier.testTag(rowTag(tag)),
                viewModel = fake,
                languageTag = tag,
                onBack = { true },
                isDownloaded = true
            )
        }

        composeRule.onNodeWithText(getFullLengthName(tag)).assertIsDisplayed()
    }

    @Test
    fun clickingDownloadIcon_callsDownload() {
        val tag = "es"
        val fake = FakeTranslationViewModel()

        composeRule.setContent {
            LanguageRow(
                modifier = Modifier.testTag(rowTag(tag)),
                viewModel = fake,
                languageTag = tag,
                onBack = { true },
                isDownloaded = false
            )
        }


        composeRule.onNodeWithTag(downloadTag(tag)).performClick()

        assertThat(fake.downloadedCalls).contains(tag)
    }

    @Test
    fun whenDownloading_showsProgressIndicator() {
        val tag = "de"
        val initial = TranslationUiState(
            currentlyDownloadingLanguageTags = MutableStateFlow(setOf(tag))
        )
        val fake = FakeTranslationViewModel(initialState = initial)

        composeRule.setContent {
            LanguageRow(
                modifier = Modifier.testTag(rowTag(tag)),
                viewModel = fake,
                languageTag = tag,
                onBack = { true },
                isDownloaded = false
            )
        }

        composeRule.onNodeWithTag(progressTag(tag)).assertIsDisplayed()
        composeRule.onNodeWithTag(downloadTag(tag)).assertDoesNotExist()
    }

    @Test
    fun clickingRow_whenDownloaded_callsSelectAndOnBack() {
        val tag = "pt"
        val fake = FakeTranslationViewModel(
            initialState = TranslationUiState(selectingLanguageRole = LanguageRole.TARGET)
        )
        var backCalled = false
        val onBack = { backCalled = true; true }

        composeRule.setContent {
            LanguageRow(
                modifier = Modifier.testTag(rowTag(tag)),
                viewModel = fake,
                languageTag = tag,
                onBack = onBack,
                isDownloaded = true
            )
        }

        composeRule.onNodeWithTag(rowTag(tag)).performClick()
        assertThat(fake.selectedTargetCalls).contains(tag)
        assertThat(backCalled).isTrue()
    }

    @Test
    fun clickingRow_whenNotDownloaded_showsDialog_andDialogDownloadCallsViewModel() {
        val tag = "fr"
        val fake = FakeTranslationViewModel()

        composeRule.setContent {
            LanguageRow(
                modifier = Modifier.testTag(rowTag(tag)),
                viewModel = fake,
                languageTag = tag,
                onBack = { true },
                isDownloaded = false
            )
        }

        composeRule.onNodeWithTag(rowTag(tag)).performClick()

        composeRule.onNodeWithTag("MODEL_MISSING_DOWNLOAD").assertIsDisplayed().performClick()

        assertThat(fake.downloadedCalls).contains(tag)
    }
}
