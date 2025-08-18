package com.example.augmentedrealityglasses.translation.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import com.example.augmentedrealityglasses.ble.permissions.PermissionBox

@Composable
fun PermissionsForTranslation(
    isMicrophoneAvailable: Boolean,
    content: @Composable () -> Unit
) {
    val permissionsForRecording =
        if (isMicrophoneAvailable) listOf(Manifest.permission.RECORD_AUDIO) else listOf() //todo if the device does not have a microphone, the feature should gracefully degrade


    PermissionBox(
        permissions = permissionsForRecording
    ) {
        content()
    }
}


