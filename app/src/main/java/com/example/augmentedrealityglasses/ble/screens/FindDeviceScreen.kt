package com.example.augmentedrealityglasses.ble.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.augmentedrealityglasses.HomeViewModel
import com.example.augmentedrealityglasses.Icon
import com.example.augmentedrealityglasses.ble.characteristic.checkBluetoothConnectPermission

@Composable
fun FindDeviceScreen(
    viewModel: HomeViewModel,
    modifier: Modifier,
    navigateOnError: () -> Unit,
    navigateOnFeatures: () -> Unit
) {
    checkBluetoothConnectPermission(LocalContext.current)

    val scanning by viewModel.isScanning.collectAsState()

    val devicesOfferingTheService by viewModel.scannedDevices.collectAsState()

    // use of remember to not change the reference of scanSettings during recomposition, which would trick the framework into considering the variable as changed
    val scanSettings: ScanSettings = remember {
        ScanSettings.Builder().setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED).build()
    }

    BluetoothScanEffect(
        viewModel,
        null,
        scanSettings,
    )

    Column(
        Modifier
            .fillMaxSize()
            .clip(shape = RoundedCornerShape(22.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Available Devices",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (scanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(27.dp),
                    strokeWidth = 3.dp,
                    color = Color.Black,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(27.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                viewModel.scan(filters = null, settings = scanSettings)
                            }
                        ), contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = Icon.REFRESH.getID()),
                        contentDescription = "refresh scanning for new devices",
                        modifier = Modifier.size(27.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (devicesOfferingTheService.isEmpty()) {
                item {
                    Text(
                        text = "No devices found",
                        fontSize = 16.sp
                    )

                }
            }

            items(devicesOfferingTheService.filter {
                it.name != null
            }) { item ->
                BluetoothDeviceItem(
                    bluetoothDevice = item,
                    onConnect = {
                        if (viewModel.connect(it)) {
                            navigateOnFeatures()
                        } else {
                            //TODO
                        }
                    },
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
internal fun BluetoothDeviceItem(
    bluetoothDevice: BluetoothDevice,
    isSampleServer: Boolean = false,
    onConnect: (BluetoothDevice) -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .clickable { onConnect(bluetoothDevice) },
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            bluetoothDevice.name ?: "N/A",
            style = if (isSampleServer) { //todo check what is sample server
                TextStyle(fontWeight = FontWeight.Bold)
            } else {
                TextStyle(fontWeight = FontWeight.Normal)
            }, fontSize = 16.sp
        )
        //Text(bluetoothDevice.address)

        /*val state = when (bluetoothDevice.bondState) {
            BluetoothDevice.BOND_BONDED -> "Paired"
            BluetoothDevice.BOND_BONDING -> "Pairing"
            else -> "None"
        }
        Text(text = state)
         */

    }
}

@SuppressLint("MissingPermission")
@Composable
private fun BluetoothScanEffect(
    viewModel: HomeViewModel,
    filters: List<ScanFilter>?,
    scanSettings: ScanSettings,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
    DisposableEffect(lifecycleOwner, filters, scanSettings) {
        val observer = LifecycleEventObserver { _, event ->
            // Start scanning once the app is in foreground and stop when in background
            if (event == Lifecycle.Event.ON_START) {
                viewModel.scan(filters = filters, settings = scanSettings)
            } else if (event == Lifecycle.Event.ON_STOP) {
                viewModel.stopScanning()
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer and stop scanning
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopScanning()
        }
    }
}