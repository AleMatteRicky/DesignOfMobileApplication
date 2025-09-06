package com.example.augmentedrealityglasses

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun UpdateWrapper(
    message: String,
    bluetoothUpdateStatus: BluetoothUpdateStatus,
    onErrorDismiss: () -> Unit,
    onBluetoothUpdateDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val durationMillis: Long = 2000
    val durationBluetoothNotification: Long = 1200
    val theme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize().background(theme.background),
        ) {
        content()

        if (bluetoothUpdateStatus != BluetoothUpdateStatus.NONE) {
            LaunchedEffect(bluetoothUpdateStatus) {
                delay(durationBluetoothNotification)
                onBluetoothUpdateDismiss()
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        Modifier.padding(start = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        val imageID: Int
                        val text: String

                        if (bluetoothUpdateStatus == BluetoothUpdateStatus.DEVICE_CONNECTED) {
                            imageID = Icon.BLUETOOTH_CONNECTED.getID()
                            text = "Device connected via Bluetooth."
                        } else {
                            imageID = Icon.BLUETOOTH_DISABLED.getID()
                            text = "Device disconnected from Bluetooth."
                        }

                        Image(
                            painter = painterResource(id = imageID),
                            contentDescription = "Bluetooth connected icon",
                            Modifier.size(20.dp)
                        )

                            if (bluetoothUpdateStatus == BluetoothUpdateStatus.DEVICE_CONNECTED) Icon.BLUETOOTH_CONNECTED.getID()
                            else Icon.BLUETOOTH_DISABLED.getID()

                        Text(
                            text = text,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        if (message.isNotEmpty()) {
            LaunchedEffect(message) {
                delay(durationMillis)
                onErrorDismiss()
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        Modifier.padding(start = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = Icon.ERROR.getID()),
                            contentDescription = "Error icon",
                            Modifier.size(16.dp)
                        )

                        Text(
                            text = message,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

enum class BluetoothUpdateStatus {
    DEVICE_CONNECTED {
    },

    DEVICE_DISCONNECTED {
    },

    NONE;
}