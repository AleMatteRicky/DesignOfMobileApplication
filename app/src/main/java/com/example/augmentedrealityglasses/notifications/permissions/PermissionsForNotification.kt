package com.example.augmentedrealityglasses.notifications.permissions

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import com.example.augmentedrealityglasses.ble.permissions.PermissionBox

@Composable
fun PermissionsForNotification(
    content: @Composable () -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        PermissionBox(
            permissions = listOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG
            ),
        ) {
            content()
        }
    }
}