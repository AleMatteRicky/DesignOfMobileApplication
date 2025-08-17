package com.example.augmentedrealityglasses.translation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.augmentedrealityglasses.Icon
import com.example.augmentedrealityglasses.translation.TranslationViewModel


@Composable
fun LanguageRow(
    modifier: Modifier,
    viewModel: TranslationViewModel,
    languageTag: String?,
    isDownloaded: Boolean
) {
    var isCurrentlyDownloading by remember { mutableStateOf(false) }

    Row(
        modifier
            .fillMaxWidth()
            .clickable { viewModel.selectTargetLanguage(languageTag) }
            .padding(
                horizontal = 16.dp,
                vertical = 14.dp
            ), verticalAlignment = Alignment.CenterVertically) {
        Text(text = if (languageTag != null) getFullLengthName(languageTag) else "-")

        Spacer(modifier = Modifier.weight(1f)) //Spacer takes all the space available

        if(languageTag != null) {
            val icon = if (isDownloaded) Icon.DOWNLOAD_COMPLETED.getID() else Icon.DOWNLOAD.getID()

            IconButton(
                onClick = {
                    if (!isDownloaded) {

                    }
                },
                modifier = Modifier
                    .size(40.dp)
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = "Download language",
                )
            }
        }
    }
}

//todo implement it in the viewModel

//private fun downloadLanguageModel(languageTag: String) {
//    RemoteModelManager.getInstance().download(
//        TranslateRemoteModel.Builder(languageTag).build(),
//        DownloadConditions.Builder().build()
//    )
//
//    val task = RemoteModelManager.getInstance().download(
//        TranslateRemoteModel.Builder(languageTag).build(),
//        DownloadConditions.Builder().build()
//    )
//
//    task.addOnSuccessListener {
//
//        isCurrentlyDownloading = false
//
//    }.addOnFailureListener { e ->
//        isCurrentlyDownloading = false
//        Log.e("Download", "failed for $languageTag", e)
//    }
//}



