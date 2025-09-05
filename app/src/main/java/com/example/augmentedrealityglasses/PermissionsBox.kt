package com.example.augmentedrealityglasses

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Composable that handles multiple runtime permissions.
 *
 * @param permissionsRequired map of <Permission, Mandatory?>
 * @param title title shown in the box
 * @param message message that explains why permissions are needed
 * @param modifier external modifier
 * @param iconId optional, shows an icon above the title
 * @param grantLabel text for the main button
 * @param onSatisfied callback triggered when all conditions are satisfied
 * @param content normal app content displayed when requirements are satisfied
 */
@Composable
fun PermissionsBox(
    modifier: Modifier = Modifier,
    permissionsRequired: Map<String, Boolean>,
    title: String = "Grant permissions",
    message: String,
    iconId: Int = R.drawable.contract,
    grantLabel: String = "Grant permissions",
    onSatisfied: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    // Track granted state for each permission
    val grantedState = remember {
        mutableStateMapOf<String, Boolean>().apply {
            permissionsRequired.keys.forEach { permissionName ->
                val isGranted = ContextCompat.checkSelfPermission(
                    context, permissionName
                ) == PackageManager.PERMISSION_GRANTED
                put(permissionName, isGranted)
            }
        }
    }

    // Track if each permission has been requested at least once
    // (to safely determine "permanently denied")
    val requestedOnce = remember {
        mutableStateMapOf<String, Boolean>().apply {
            permissionsRequired.keys.forEach { permissionName -> put(permissionName, false) }
        }
    }

    // Helper to refresh current grant state (used on resume)
    fun refreshGrants() {
        permissionsRequired.keys.forEach { permissionName ->
            val isGranted = ContextCompat.checkSelfPermission(
                context, permissionName
            ) == PackageManager.PERMISSION_GRANTED
            grantedState[permissionName] = isGranted
        }
    }

    // Refresh every time we return to foreground (for example: back from Settings)
    DisposableEffect(lifecycleOwner, permissionsRequired) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshGrants()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Launcher to request multiple permissions
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // Mark requested and update grants
        result.forEach { (permissionName, isGranted) ->
            requestedOnce[permissionName] = true
            grantedState[permissionName] = isGranted
        }
    }

    // Check if all mandatory permissions are granted
    val allMandatoryAccepted by remember(grantedState) {
        derivedStateOf {
            permissionsRequired
                .filter { it.value }
                .all { (permissionName, _) ->
                    grantedState[permissionName] == true
                }
        }
    }

    // At least one permission from the list must be granted
    val atLeastOneAccepted by remember(grantedState) {
        derivedStateOf {
            permissionsRequired.keys.any { permissionName ->
                grantedState[permissionName] == true
            }
        }
    }

    // Final condition
    val satisfied: Boolean by remember(allMandatoryAccepted, atLeastOneAccepted) {
        derivedStateOf { allMandatoryAccepted && atLeastOneAccepted }
    }

    // Determine if at least one permission is "permanently denied"
    val hasPermanentlyDenied by remember(grantedState, requestedOnce) {
        derivedStateOf {
            permissionsRequired.keys.any { perm ->
                val granted = grantedState[perm] == true
                val asked = requestedOnce[perm] == true
                val shouldShowRationale =
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
                !granted && asked && !shouldShowRationale
            }
        }
    }

    LaunchedEffect(satisfied) {
        if (satisfied) {
            onSatisfied()
        }
    }

    if (satisfied) {
        // Show normal screen content
        content()
    } else {
        // Show permissions screen
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    //TODO: adjust colors
                    val titleColor = Color(0xFF111827)
                    val textColor = Color(0xFF6B7280)

                    Icon(
                        painter = painterResource(id = iconId),
                        contentDescription = null,
                        modifier = Modifier.size(84.dp)
                    )
                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = titleColor
                        ),

                        )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = textColor
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    PermissionStatusList(
                        permissionsRequired = permissionsRequired,
                        grantedState = grantedState,
                        onPermissionClick = {
                            launcher.launch(arrayOf(it))
                        }
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {

                    if (!hasPermanentlyDenied) {
                        Button(
                            onClick = {
                                launcher.launch(permissionsRequired.keys.toTypedArray())
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black,
                                contentColor = Color.White
                            )
                        ) {
                            Text(grantLabel)
                        }
                    } else {
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", activity.packageName, null)
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                activity.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Open app settings",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusList(
    permissionsRequired: Map<String, Boolean>,
    grantedState: Map<String, Boolean>,
    onPermissionClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        permissionsRequired.forEach { (perm, mandatory) ->
            val granted = grantedState[perm] == true
            val lineColor = when {
                granted -> Color.Green
                mandatory -> Color.Red
                else -> Color.Gray
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable(
                        onClick = {
                            if (!granted) {
                                onPermissionClick(perm)
                            }
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(lineColor, RoundedCornerShape(50))
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = perm.substringAfterLast('.')
                        .replace('_', ' ')
                        .lowercase()
                        .replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 18.sp,
                        color = Color(0xFF111827)
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = when {
                        granted -> "Granted"
                        mandatory -> "Required"
                        else -> "Optional"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280)
                    )
                )
            }
        }
    }
}