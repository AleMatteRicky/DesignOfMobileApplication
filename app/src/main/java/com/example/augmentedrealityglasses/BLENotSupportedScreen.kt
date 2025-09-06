package com.example.augmentedrealityglasses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BLENotSupportedScreen() {

    val theme = MaterialTheme.colorScheme
    val titleColor = theme.primary
    val textColor = theme.secondary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(theme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.info),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = theme.primary
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Bluetooth LE not supported",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 20.sp
                ),
                color = titleColor,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "This device does not support Bluetooth Low Energy, required for scanning and connecting to the glasses. However, you can still use features such as translation and weather. These will run only on this device without sending data to the glasses.",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}