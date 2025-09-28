package com.example.augmentedrealityglasses.translation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun DisplayModelDownloading(text: String) {

    val colorScheme = MaterialTheme.colorScheme

    Dialog(onDismissRequest = { /* leaving this function empty avoids that the user close the dialog only by clicking outside it */ }) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 8.dp,
            color = colorScheme.primaryContainer
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .background(colorScheme.primaryContainer)
                    .width(IntrinsicSize.Min),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text,
                    color = colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium //todo adjust
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = colorScheme.primary,
                    trackColor = colorScheme.inversePrimary //todo check why it is not white in dark mode
                )
            }
        }
    }
}