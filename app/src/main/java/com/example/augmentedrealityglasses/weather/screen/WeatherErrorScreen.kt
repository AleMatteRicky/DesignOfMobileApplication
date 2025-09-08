package com.example.augmentedrealityglasses.weather.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WeatherErrorScreen(
    msg: String,
    iconId: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Something went wrong",
    retryLabel: String = "Retry",
) {
    val theme = MaterialTheme.colorScheme
    val titleColor = theme.primary
    val textColor = theme.secondary

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            Icon(
                painter = painterResource(id = iconId),
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = theme.primary
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 20.sp
                ),
                color = titleColor,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth(if (isLandscape) 0.45f else 1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.onSurface,
                        contentColor = theme.inversePrimary
                    )
                ) {
                    Text(
                        text = retryLabel,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}