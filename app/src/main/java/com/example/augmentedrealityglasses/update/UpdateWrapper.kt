package com.example.augmentedrealityglasses.update

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    var previousStatus by remember { mutableStateOf(BluetoothUpdateStatus.NONE) }
    var bluetoothBannerText by remember { mutableStateOf<String?>(null) }
    var bluetoothBannerIconId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(bluetoothUpdateStatus) {
        val prev = previousStatus

        if (prev != BluetoothUpdateStatus.NONE &&
            bluetoothUpdateStatus != BluetoothUpdateStatus.NONE &&
            prev != bluetoothUpdateStatus
        ) {
            if (bluetoothUpdateStatus == BluetoothUpdateStatus.DEVICE_CONNECTED) {
                bluetoothBannerText = "Device connected via Bluetooth."
                bluetoothBannerIconId = com.example.augmentedrealityglasses.ui.theme.Icon.BLUETOOTH_CONNECTED.getID()
            } else {
                bluetoothBannerText = "Device disconnected from Bluetooth."
                bluetoothBannerIconId = com.example.augmentedrealityglasses.ui.theme.Icon.BLUETOOTH_DISABLED.getID()
            }
        }

        previousStatus = bluetoothUpdateStatus
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background),
    ) {
        content()

        if (bluetoothBannerText != null && bluetoothBannerIconId != null) {
            LaunchedEffect(bluetoothBannerText) {
                delay(durationBluetoothNotification)
                bluetoothBannerText = null
                bluetoothBannerIconId = null
                onBluetoothUpdateDismiss()
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    color = theme.onSurface,
                    shape = RoundedCornerShape(8.dp),
                    //shadowElevation = 4.dp
                ) {
                    Row(
                        Modifier.padding(start = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            painter = painterResource(id = bluetoothBannerIconId!!),
                            tint = theme.inversePrimary,
                            contentDescription = "Bluetooth connected icon",
                            modifier = Modifier.size(20.dp),
                        )

                        Text(
                            text = bluetoothBannerText!!,
                            color = theme.inversePrimary,
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
                    color = theme.onSurface,
                    shape = RoundedCornerShape(8.dp),
                    //shadowElevation = 4.dp
                ) {
                    Row(
                        Modifier.padding(start = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = com.example.augmentedrealityglasses.ui.theme.Icon.ERROR.getID()),
                            contentDescription = "Error icon",
                            Modifier.size(16.dp)
                        )

                        Text(
                            text = message,
                            color = theme.inversePrimary,
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