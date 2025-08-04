package com.example.augmentedrealityglasses.ble.characteristic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.augmentedrealityglasses.ble.checkOtherwiseExec

fun checkBluetoothConnectPermission(
    context: Context
) {
    checkOtherwiseExec(
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
    ) {
        throw SecurityException("Missing")
    }
}