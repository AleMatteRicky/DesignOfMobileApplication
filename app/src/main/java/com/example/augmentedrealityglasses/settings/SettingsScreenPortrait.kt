package com.example.augmentedrealityglasses.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.augmentedrealityglasses.UpdateWrapper
import com.example.augmentedrealityglasses.notifications.ChatNotificationListenerService

//todo check interaction shadow border
@Composable
fun SettingsScreenPortrait(
    viewModel: SettingsViewModel,
    isChangeThemeClicked: MutableState<Boolean>
) {
    //Main UI state
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val useSystem = uiState.theme == ThemeMode.SYSTEM

    val isDarkSelected = when (uiState.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    //Access of the application to read phone notifications
    val hasNotifyAccess by rememberNotificationAccessState()

    val theme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        viewModel.receiveBLEUpdates()
        //isChangeThemeClicked.value = true
    }

    UpdateWrapper(
        message = viewModel.errorMessage,
        bluetoothUpdateStatus = viewModel.bluetoothUpdateStatus,
        onErrorDismiss = { viewModel.hideErrorMessage() },
        onBluetoothUpdateDismiss = { viewModel.hideBluetoothUpdate() }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.background)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(24.dp))

            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = theme.primary
            )

            Spacer(Modifier.height(24.dp))

            AppearancePanel(
                useSystemThemeMode = useSystem,
                isDarkSelected = isDarkSelected,
                onSystemToggle = { viewModel.onSystemToggle(it) },
                onSelectDark = { viewModel.onSelectDark() },
                onSelectLight = { viewModel.onSelectLight() },
                isChangeThemeClicked = isChangeThemeClicked
            )

            Spacer(Modifier.height(24.dp))

            NotificationFiltersPanel(
                hasNotificationAccess = hasNotifyAccess,
                notificationEnabled = uiState.notificationEnabled,
                onEnableNotificationSource = { viewModel.onEnableNotificationSource(it) },
                onDisableNotificationSource = { viewModel.onDisableNotificationSource(it) }
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun AppearancePanel(
    useSystemThemeMode: Boolean,
    isDarkSelected: Boolean,
    onSystemToggle: (Boolean) -> Unit,
    onSelectDark: () -> Unit,
    onSelectLight: () -> Unit,
    isChangeThemeClicked: MutableState<Boolean>
) {
    val theme = MaterialTheme.colorScheme

    Text(
        text = "Appearance",
        style = MaterialTheme.typography.titleSmall.copy(
            fontSize = 22.sp
        ),
        color = theme.primary
    )

    Spacer(Modifier.height(10.dp))

    Card(
        shape = RoundedCornerShape(16.dp),
        //elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = theme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = useSystemThemeMode,
                        onValueChange = {
                            isChangeThemeClicked.value = true
                            onSystemToggle(it)
                        }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically

            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Use device setting",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 18.sp
                        ),
                        color = theme.primary
                    )
                    Text(
                        "Follow the device theme",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.secondary
                    )
                }
                Checkbox(
                    checked = useSystemThemeMode,
                    onCheckedChange = null
                )
            }

            HorizontalDivider(color = Color.LightGray) //TODO: adjust color

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        enabled = !useSystemThemeMode,
                        value = isDarkSelected,
                        onValueChange = { checked ->
                            isChangeThemeClicked.value = true
                            if (checked) onSelectDark() else onSelectLight()
                        }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Dark mode",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 18.sp
                        ),
                        color = theme.primary
                    )
                    val subtitle = if (isDarkSelected) "Currently Dark"
                    else "Currently Light"
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.secondary
                    )
                }

                Switch(
                    enabled = !useSystemThemeMode,
                    checked = isDarkSelected,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = theme.inverseSurface,
                        checkedTrackColor = theme.inversePrimary,
                        checkedBorderColor = theme.secondary,

                        uncheckedThumbColor = theme.secondary,
                        uncheckedTrackColor = theme.inverseSurface,
                        uncheckedBorderColor = theme.secondary,


                        disabledCheckedThumbColor = Color.LightGray,
                        disabledCheckedTrackColor = theme.inversePrimary,
                        disabledCheckedBorderColor = Color.LightGray,

                        disabledUncheckedThumbColor = Color.LightGray,
                        disabledUncheckedTrackColor = theme.inverseSurface,
                        disabledUncheckedBorderColor = Color.LightGray,
                    )
                )
            }
        }
    }
}

@Composable
fun NotificationFiltersPanel(
    hasNotificationAccess: Boolean,
    notificationEnabled: Map<NotificationSource, Boolean>,
    onEnableNotificationSource: (NotificationSource) -> Unit,
    onDisableNotificationSource: (NotificationSource) -> Unit
) {
    val theme = MaterialTheme.colorScheme

    Text(
        text = "Notification filters",
        style = MaterialTheme.typography.titleSmall.copy(
            fontSize = 22.sp
        ),
        color = theme.primary
    )

    Spacer(Modifier.height(5.dp))

    Text(
        text = "Choose which notifications to forward to your glasses",
        style = MaterialTheme.typography.bodySmall,
        color = theme.secondary
    )

    Spacer(Modifier.height(10.dp))

    Card(
        shape = RoundedCornerShape(16.dp),
        //elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = theme.tertiaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AppToggleRow(
                title = "Phone call",
                checked = notificationEnabled[NotificationSource.CALL] ?: false,
                onCheckedChange = { checked ->
                    if (checked) onEnableNotificationSource(NotificationSource.CALL)
                    else onDisableNotificationSource(NotificationSource.CALL)
                }
            )

            HorizontalDivider(color = Color.LightGray) //TODO: adjust color?

            AppToggleRow(
                title = "Sms",
                checked = notificationEnabled[NotificationSource.SMS] ?: false,
                onCheckedChange = { checked ->
                    if (checked) onEnableNotificationSource(NotificationSource.SMS)
                    else onDisableNotificationSource(NotificationSource.SMS)
                }
            )

            HorizontalDivider(color = Color.LightGray) //TODO: adjust color or remove it?

            if (!hasNotificationAccess) {
                NotificationAccessPanel()
            } else {
                AppToggleRow(
                    title = "WhatsApp",
                    checked = notificationEnabled[NotificationSource.WHATSAPP] ?: false,
                    onCheckedChange = { checked ->
                        if (checked) onEnableNotificationSource(NotificationSource.WHATSAPP)
                        else onDisableNotificationSource(NotificationSource.WHATSAPP)
                    }
                )

                HorizontalDivider(color = Color.LightGray) //TODO: adjust color?

                AppToggleRow(
                    title = "Telegram",
                    checked = notificationEnabled[NotificationSource.TELEGRAM] ?: false,
                    onCheckedChange = { checked ->
                        if (checked) onEnableNotificationSource(NotificationSource.TELEGRAM)
                        else onDisableNotificationSource(NotificationSource.TELEGRAM)
                    }
                )

                HorizontalDivider(color = Color.LightGray) //TODO: adjust color?

                AppToggleRow(
                    title = "Gmail",
                    checked = notificationEnabled[NotificationSource.GMAIL] ?: false,
                    onCheckedChange = { checked ->
                        if (checked) onEnableNotificationSource(NotificationSource.GMAIL)
                        else onDisableNotificationSource(NotificationSource.GMAIL)
                    }
                )

                HorizontalDivider(color = Color.LightGray) //TODO: adjust color?

                AppToggleRow(
                    title = "Outlook",
                    checked = notificationEnabled[NotificationSource.OUTLOOK] ?: false,
                    onCheckedChange = { checked ->
                        if (checked) onEnableNotificationSource(NotificationSource.OUTLOOK)
                        else onDisableNotificationSource(NotificationSource.OUTLOOK)
                    }
                )
            }
        }
    }
}

@Composable
private fun AppToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val theme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 18.sp
                ),
                color = theme.primary
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = theme.inverseSurface,
                checkedTrackColor = theme.primary,
                checkedBorderColor = theme.secondary,

                uncheckedThumbColor = theme.secondary,
                uncheckedTrackColor = theme.inverseSurface,
                uncheckedBorderColor = theme.secondary,
            )
        )
    }
}

/**
 * This composable allows to update the screen after the user visited the phone settings in order to activate notification access
 */
@Composable
fun rememberNotificationAccessState(): State<Boolean> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember { mutableStateOf(hasNotificationAccess(context)) }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state.value = hasNotificationAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    return state
}

@Composable
fun NotificationAccessPanel() {
    val context = LocalContext.current
    val theme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.surfaceBright, RoundedCornerShape(12.dp)) //TODO: adjust color
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Notification access required",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 18.sp
            ),
            color = theme.primary
        )
        Text(
            text = "Enable notification access to forward applications messages.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray //TODO: adjust color
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = theme.tertiaryContainer),
                onClick = {
                    context.startActivity(
                        Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }) {
                Text(
                    text = "Open settings",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp
                    ),
                    color = theme.primary
                )
            }
        }
    }
}

/**
 * This function check whether the application is allowed or not to read notifications
 */
fun hasNotificationAccess(context: Context): Boolean {
    val cn = ComponentName(context, ChatNotificationListenerService::class.java)
    val flat = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )
    return flat?.split(":")?.any { it.equals(cn.flattenToString(), ignoreCase = true) } == true
}