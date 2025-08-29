package com.example.augmentedrealityglasses.notifications.permissions

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import com.example.augmentedrealityglasses.ble.permissions.PermissionBox

@Composable
fun PermissionsForNotification(
    isDeviceSmsCapable: Boolean,
    content: @Composable () -> Unit
) {
    val permissionsForCallNotifications = listOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_CONTACTS
    )

    val permissionsForMessageNotifications =
        if (isDeviceSmsCapable) listOf(Manifest.permission.RECEIVE_SMS) else listOf()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        PermissionBox(
            permissions = permissionsForCallNotifications + permissionsForMessageNotifications
        ) {
            content()
        }
    }
}