package com.example.augmentedrealityglasses.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.augmentedrealityglasses.notifications.ChatNotificationListenerService

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
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
    val hasNotifAccess by rememberNotificationAccessState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6F7)) //FIXME: fix color
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(20.dp))

        AppearancePanel(
            useSystemThemeMode = useSystem,
            isDarkSelected = isDarkSelected,
            onSystemToggle = { viewModel.onSystemToggle(it) },
            onSelectDark = { viewModel.onSelectDark() },
            onSelectLight = { viewModel.onSelectLight() }
        )

        Spacer(Modifier.height(15.dp))

        if (!hasNotifAccess) {
            NotificationAccessPanel()
        } else {
            NotificationPreferencesPanel(
                notificationEnabled = uiState.notificationEnabled,
                onEnableNotificationSource = { viewModel.onEnableNotificationSource(it) },
                onDisableNotificationSource = { viewModel.onDisableNotificationSource(it) }
            )
        }
    }
}

@Composable
fun AppearancePanel(
    useSystemThemeMode: Boolean,
    isDarkSelected: Boolean,
    onSystemToggle: (Boolean) -> Unit,
    onSelectDark: () -> Unit,
    onSelectLight: () -> Unit
) {
    Text(
        text = "Appearance",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(Modifier.height(10.dp))

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Use system setting", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Follow the device theme",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Checkbox(
                    checked = useSystemThemeMode,
                    onCheckedChange = { onSystemToggle(it) }
                )
            }

            HorizontalDivider(color = Color(0xFFE8E8E8))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Dark mode", fontWeight = FontWeight.SemiBold)
                    val subtitle = if (useSystemThemeMode) "System theme"
                    else if (isDarkSelected) "Currently Dark"
                    else "Currently Light"
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Switch(
                    enabled = !useSystemThemeMode,
                    checked = isDarkSelected,
                    onCheckedChange = { checked ->
                        if (checked) onSelectDark() else onSelectLight()
                    }
                )
            }
        }
    }
}

@Composable
fun NotificationPreferencesPanel(
    notificationEnabled: Map<NotificationSource, Boolean>,
    onEnableNotificationSource: (NotificationSource) -> Unit,
    onDisableNotificationSource: (NotificationSource) -> Unit
) {
    Text(
        text = "Notification filters",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(Modifier.height(10.dp))

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppToggleRow(
                title = "Phone call",
                subtitle = "Forward call notifications to your device",
                checked = notificationEnabled[NotificationSource.CALL] ?: false,
                onCheckedChange = { checked ->
                    if (checked) onEnableNotificationSource(NotificationSource.CALL)
                    else onDisableNotificationSource(NotificationSource.CALL)
                }
            )

            HorizontalDivider(color = Color(0xFFE8E8E8))

            AppToggleRow(
                title = "Sms",
                subtitle = "Forward sms notifications to your device",
                checked = notificationEnabled[NotificationSource.SMS] ?: false,
                onCheckedChange = { checked ->
                    if (checked) onEnableNotificationSource(NotificationSource.SMS)
                    else onDisableNotificationSource(NotificationSource.SMS)
                }
            )

            HorizontalDivider(color = Color(0xFFE8E8E8))

            AppToggleRow(
                title = "WhatsApp",
                subtitle = "Forward WhatsApp notifications to your device",
                checked = notificationEnabled[NotificationSource.WHATSAPP] ?: false,
                onCheckedChange = { checked ->
                    if (checked) onEnableNotificationSource(NotificationSource.WHATSAPP)
                    else onDisableNotificationSource(NotificationSource.WHATSAPP)
                }
            )

            HorizontalDivider(color = Color(0xFFE8E8E8))

            AppToggleRow(
                title = "Telegram",
                subtitle = "Forward Telegram notifications to your device",
                checked = notificationEnabled[NotificationSource.TELEGRAM] ?: false,
                onCheckedChange = { checked ->
                    if (checked) onEnableNotificationSource(NotificationSource.TELEGRAM)
                    else onDisableNotificationSource(NotificationSource.TELEGRAM)
                }
            )
        }
    }
}

@Composable
private fun AppToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        Switch(
            enabled = true,
            checked = checked,
            onCheckedChange = onCheckedChange
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

    Text(
        text = "Notification filters",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(Modifier.height(10.dp))

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Enable access to read app notifications", fontWeight = FontWeight.SemiBold)
            Text(
                "Required to forward WhatsApp/Telegram notifications to your glasses.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                //This button redirect to phone settings
                Button(
                    onClick = {
                        context.startActivity(
                            Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }) {
                    Text("Open settings")
                }
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