package com.example.augmentedrealityglasses.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.augmentedrealityglasses.UpdateWrapper

@Composable
fun SettingsScreenLandscape(
    viewModel: SettingsViewModel,
    isChangeThemeClicked: MutableState<Boolean>
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val useSystem = uiState.theme == ThemeMode.SYSTEM

    val isDarkSelected = when (uiState.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val hasNotifyAccess by rememberNotificationAccessState()

    val theme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        viewModel.receiveBLEUpdates()
    }

    UpdateWrapper(
        message = viewModel.errorMessage,
        bluetoothUpdateStatus = viewModel.bluetoothUpdateStatus,
        onErrorDismiss = { viewModel.hideErrorMessage() },
        onBluetoothUpdateDismiss = { viewModel.hideBluetoothUpdate() }
    ) {
        Scaffold(
            containerColor = theme.background,
        ) { innerPadding ->

            val leftScroll = rememberScrollState()
            val rightScroll = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(theme.background)
                    .padding(innerPadding)
            ) {
                Column(Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = theme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(leftScroll)
                        ) {
                            AppearancePanel(
                                useSystemThemeMode = useSystem,
                                isDarkSelected = isDarkSelected,
                                onSystemToggle = { viewModel.onSystemToggle(it) },
                                onSelectDark = { viewModel.onSelectDark() },
                                onSelectLight = { viewModel.onSelectLight() },
                                isChangeThemeClicked = isChangeThemeClicked
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rightScroll)
                        ) {
                            NotificationFiltersPanel(
                                hasNotificationAccess = hasNotifyAccess,
                                notificationEnabled = uiState.notificationEnabled,
                                onEnableNotificationSource = {
                                    viewModel.onEnableNotificationSource(
                                        it
                                    )
                                },
                                onDisableNotificationSource = {
                                    viewModel.onDisableNotificationSource(
                                        it
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}