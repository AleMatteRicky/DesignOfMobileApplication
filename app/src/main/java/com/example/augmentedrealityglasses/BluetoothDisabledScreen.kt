package com.example.augmentedrealityglasses

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun BluetoothDisabledScreen(
    onEnabled: () -> Unit
) {
    val result =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                onEnabled()
            }
        }

    //TODO: adjust colors
    val titleColor = Color(0xFF111827)
    val textColor = Color(0xFF6B7280)

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
                modifier = Modifier.size(96.dp)
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Bluetooth disabled",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = titleColor,
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(15.dp))

            //TODO: refine text
            Text(
                text = "It seems that bluetooth is disabled. Turn on it in order to access the homepage.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = textColor,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.weight(1f))

            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        result.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
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