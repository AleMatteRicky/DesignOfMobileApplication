package com.example.augmentedrealityglasses.ble.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.augmentedrealityglasses.HomeViewModel
import com.example.augmentedrealityglasses.ble.characteristic.checkBluetoothConnectPermission

@Composable
fun FindDeviceScreen(
    viewModel: HomeViewModel, navigateOnError: () -> Unit, navigateOnFeatures: () -> Unit
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

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Available devices", style = MaterialTheme.typography.titleSmall)
            if (scanning) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(
                    onClick = {
                        viewModel.scan(filters = null, settings = scanSettings)
                    },
                ) {
                    Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (devicesOfferingTheService.isEmpty()) {
                item {
                    Text(text = "No devices found")
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
            style = if (isSampleServer) {
                TextStyle(fontWeight = FontWeight.Bold)
            } else {
                TextStyle(fontWeight = FontWeight.Normal)
            },
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