package com.example.augmentedrealityglasses.home

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.augmentedrealityglasses.R

@Composable
fun BluetoothDisabledScreen(
    onEnabled: () -> Unit
) {
    val theme = MaterialTheme.colorScheme
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val result =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                onEnabled()
            }
        }

    val titleColor = theme.primary
    val textColor = theme.secondary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            Icon(
                painter = painterResource(id = R.drawable.bluetooth_disabled),
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = theme.primary
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Bluetooth disabled",
                style = MaterialTheme.typography.titleLarge,
                color = titleColor,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(15.dp))

            Text(
                text = "It seems that bluetooth is disabled. Turn on it in order to access the homepage and connect your device",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        result.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    },
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
                        text = "Enable",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}