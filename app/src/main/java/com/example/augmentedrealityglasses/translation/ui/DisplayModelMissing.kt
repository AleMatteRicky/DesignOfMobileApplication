package com.example.augmentedrealityglasses.translation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun DisplayModelMissing(onClickDownload: () -> Unit, resetVisibility: () -> Unit) {
    var showDialog by remember { mutableStateOf(true) }
    if (showDialog) {
        Dialog(onDismissRequest = {
            resetVisibility()
            showDialog = false
        }) {
            Surface(
                shape = RoundedCornerShape(15.dp),
                tonalElevation = 10.dp,
                color = Color.White,
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(0.7f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .width(IntrinsicSize.Min),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "This model is not available on the device, download it in order to translate in this language",
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp)) //could be removed
                    Button(
                        onClickDownload, colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Download", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}