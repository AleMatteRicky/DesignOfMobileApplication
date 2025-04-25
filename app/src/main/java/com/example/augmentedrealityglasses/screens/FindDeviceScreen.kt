package com.example.augmentedrealityglasses.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.augmentedrealityglasses.TAG
import com.example.augmentedrealityglasses.ble.device.SERVICE_UUID
import kotlinx.coroutines.delay

@Composable
fun FindDeviceScreen(onError: () -> Unit, onConnect: (BluetoothDevice) -> Unit) {
    val notGranted = ActivityCompat.checkSelfPermission(
        LocalContext.current,
        Manifest.permission.BLUETOOTH_CONNECT
    ) != PackageManager.PERMISSION_GRANTED

    require(
        !notGranted
    )

    val context = LocalContext.current
    val bluetoothManager: BluetoothManager =
        checkNotNull(context.getSystemService(BluetoothManager::class.java))
    val adapter: BluetoothAdapter? = bluetoothManager.getAdapter()
    if (adapter == null) {
        Log.d(TAG, "adapter is null => the device does not support bluetooth")
        // TODO: for now left as is. Generally, the current screen should be popped out.
        onError()
        return
    }

    // variable to know whether to continue scanning or not
    var scanning by remember {
        mutableStateOf(true)
    }

    // TODO. add virtual view to manage the state
    val myConnect: (BluetoothDevice) -> Unit = {
        scanning = false
        onConnect(it)
    }

    val devices = remember {
        mutableStateListOf<BluetoothDevice>()
    }

    val pairedDevices = remember {
        mutableStateListOf(*adapter.bondedDevices.toTypedArray())
    }

    // TODO: use as a single value
    val serverDevices = remember {
        mutableStateListOf<BluetoothDevice>()
    }
    val scanSettings: ScanSettings = ScanSettings.Builder()
        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .build()

    // TODO: change the function to start the connection with the selected device
    if (scanning) {
        BluetoothScanEffect(
            scanSettings = scanSettings,
            onScanFailed = {
                scanning = false
                Log.w(TAG, "Scan failed with error: $it")
            },
            onDeviceFound = { scanResult ->
                Log.d(
                    TAG,
                    "found device: name -> ${scanResult.device.name} address -> ${scanResult.device.address}"
                )
                if (!devices.contains(scanResult.device)) {
                    devices.add(scanResult.device)
                }

                // If we find our GATT server sample let's highlight it
                val serviceUuids = scanResult.scanRecord?.serviceUuids.orEmpty()
                if (serviceUuids.contains(ParcelUuid(SERVICE_UUID))) {
                    if (!serverDevices.contains(scanResult.device)) {
                        serverDevices.add(scanResult.device)
                    }
                }
            },
        )

        // to not execute the code again when the UI recomposes
        LaunchedEffect(Unit) {
            delay(15000)
            scanning = false
        }
    }

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
                        devices.clear()
                        scanning = true
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
            if (devices.isEmpty()) {
                item {
                    Text(text = "No devices found")
                }
            }
            items(devices) { item ->
                BluetoothDeviceItem(
                    bluetoothDevice = item,
                    isSampleServer = serverDevices.contains(item),
                    onConnect = myConnect,
                )
            }

            if (pairedDevices.isNotEmpty()) {
                item {
                    Text(text = "Saved devices", style = MaterialTheme.typography.titleSmall)
                }
                items(pairedDevices) {
                    BluetoothDeviceItem(
                        bluetoothDevice = it,
                        onConnect = myConnect,
                    )
                }
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
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            bluetoothDevice.name ?: "N/A",
            style = if (isSampleServer) {
                TextStyle(fontWeight = FontWeight.Bold)
            } else {
                TextStyle(fontWeight = FontWeight.Normal)
            },
        )
        Text(bluetoothDevice.address)
        val state = when (bluetoothDevice.bondState) {
            BluetoothDevice.BOND_BONDED -> "Paired"
            BluetoothDevice.BOND_BONDING -> "Pairing"
            else -> "None"
        }
        Text(text = state)

    }
}

@SuppressLint("MissingPermission")
@Composable
private fun BluetoothScanEffect(
    scanSettings: ScanSettings,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onScanFailed: (Int) -> Unit,
    onDeviceFound: (device: ScanResult) -> Unit,
) {
    val context = LocalContext.current
    val adapter = context.getSystemService(BluetoothManager::class.java).adapter

    if (adapter == null) {
        onScanFailed(ScanCallback.SCAN_FAILED_INTERNAL_ERROR)
        return
    }

    val currentOnDeviceFound by rememberUpdatedState(onDeviceFound)

    DisposableEffect(lifecycleOwner, scanSettings) {
        val leScanCallback: ScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                currentOnDeviceFound(result)
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                onScanFailed(errorCode)
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            // Start scanning once the app is in foreground and stop when in background
            if (event == Lifecycle.Event.ON_START) {
                adapter.bluetoothLeScanner.startScan(null, scanSettings, leScanCallback)
            } else if (event == Lifecycle.Event.ON_STOP) {
                adapter.bluetoothLeScanner.stopScan(leScanCallback)
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer and stop scanning
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adapter.bluetoothLeScanner.stopScan(leScanCallback)
        }
    }
}