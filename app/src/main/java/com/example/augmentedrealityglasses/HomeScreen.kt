package com.example.augmentedrealityglasses

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateFindDevice: () -> Unit
) {

    val context = LocalContext.current
    val bluetoothManager: BluetoothManager =
        checkNotNull(context.getSystemService(BluetoothManager::class.java))
    val adapter: BluetoothAdapter? = bluetoothManager.getAdapter()

    Column {
        Text(
            text = "Home",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        DevicesPanel(
            viewModel.isExtDeviceConnected
        ) { viewModel.disconnectDevice() }

        AddDevicesButton(
            onNavigateFindDevice
        )
    }
}

@Composable
fun DevicesPanel(
    connected: Boolean,
    onDeviceStatusPanelClick: () -> Unit
) {
    Column {
        Text(
            text = "Your Devices",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        DeviceStatusPanel(
            connected = connected,
            onClick = onDeviceStatusPanelClick
        )
        DevicesListPanel()
    }
}

@Composable
fun DeviceStatusPanel(
    connected: Boolean,
    onClick: () -> Unit
) {
    if (connected) {
        Box(
            modifier = Modifier.padding(13.dp)
        ) {
            Card(
                onClick = onClick,
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF1F5F9),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.eyeglasses),
                            contentDescription = null,
                            modifier = Modifier.size(58.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Currently\nconnected",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            lineHeight = MaterialTheme.typography.titleLarge.lineHeight,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Click here to disconnect",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4B5563),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    } else {
        Text("No connected device")
    }
}

@Composable
fun DevicesListPanel() {
    Box(
        modifier = Modifier.padding(13.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(

            ) {
                Text(
                    text = "Previously connected devices",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun AddDevicesButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 13.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        FloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = Color.Black,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 8.dp
            ),
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add device",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}