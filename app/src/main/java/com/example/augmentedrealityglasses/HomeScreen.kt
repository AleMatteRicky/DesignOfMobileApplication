package com.example.augmentedrealityglasses

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.augmentedrealityglasses.ble.ESP32Proxy
import com.example.augmentedrealityglasses.ble.screens.FindDeviceScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    //TODO: delete this parameter and also his ScreenName value?
    onNavigateFindDevice: () -> Unit,
    navigationBarHeight: Dp
) {
    val colorScheme = MaterialTheme.colorScheme
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    val screenWidthDp = configuration.screenWidthDp.dp
    val context = LocalContext.current
    val bluetoothManager = remember(context) {
        checkNotNull(context.getSystemService(BluetoothManager::class.java))
    }
    val adapter: BluetoothAdapter? = remember(bluetoothManager) {
        bluetoothManager.adapter
    }

    var showFindDevicePanel = remember { mutableStateOf(false) }


    //FIXME: allow users to refresh data?
    LaunchedEffect(Unit) {
        viewModel.refreshBondedDevices(context, adapter)
    }
    UpdateWrapper(
        message = viewModel.errorMessage,
        bluetoothUpdateStatus = viewModel.bluetoothUpdateStatus,
        onErrorDismiss = { viewModel.hideErrorMessage() },
        onBluetoothUpdateDismiss = { viewModel.hideBluetoothUpdate() }
    ) {

        val modifier = if (showFindDevicePanel.value) Modifier
            .background(Color.Transparent)
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (showFindDevicePanel.value) {
                    showFindDevicePanel.value = false
                }
            } else Modifier.fillMaxSize()

        BoxWithConstraints(
            modifier = modifier.fillMaxSize()
        ) {

            Box(
                modifier = if (showFindDevicePanel.value) {
                    Modifier
                        .fillMaxSize()
                        .blur(20.dp)
                } else {
                    Modifier.fillMaxSize()
                }
            ) {

                Column {
                    DevicesPanel(
                        //TODO: do not show the connected device in the "Previously connected devices" panel
                        devices = viewModel.bondedDevices,
                        connected = viewModel.isExtDeviceConnected,
                        onDeviceClick = {
                            viewModel.tryToConnectBondedDevice(
                                device = it
                            )
                        },
                        onDeviceStatusPanelClick = { viewModel.disconnectDevice() }
                    )
                }
            }

            SharedTransitionLayout(Modifier.fillMaxSize()) {

                val sharedState = rememberSharedContentState(key = "find_device_transition")
                val boxHeight = screenHeightDp * 0.6f
                val boxWidth = maxWidth * 0.9f

                AnimatedContent(targetState = showFindDevicePanel.value) { targetState ->
                    if (!targetState) {

                        val offsetWithRecordButton = (65.dp * 1.1f - 65.dp) / 2
                        val addButtonBoxYOffset =
                            maxHeight - (65.dp * 1.2f + 10.dp) + offsetWithRecordButton

                        Box(
                            Modifier
                                .offset(
                                    x = (maxWidth - 65.dp) / 2,
                                    y = addButtonBoxYOffset
                                )
                                .sharedBounds(
                                    sharedContentState = sharedState,
                                    animatedVisibilityScope = this@AnimatedContent,
                                    resizeMode = ResizeMode.ScaleToBounds(),
                                )
                                .padding(bottom = 13.dp)

                        ) {
                            AddDevicesButton(
                                { showFindDevicePanel.value = true },
                                modifier = Modifier, showFindDevicePanel.value
                            )
                        }
                    } else {

                        Box(
                            Modifier
                                .offset(
                                    x = (maxWidth - boxWidth) / 2,
                                    y = (screenHeightDp - boxHeight) / 2 - 50.dp
                                )
                                .background(Color.Transparent)
                                .zIndex(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {})
                                .sharedBounds(
                                    sharedContentState = sharedState,
                                    animatedVisibilityScope = this@AnimatedContent,
                                    resizeMode = ResizeMode.ScaleToBounds(),
                                )
                                .height(boxHeight + 60.dp)

                        ) {
                            Column(
                                Modifier
                                    .background(Color.Transparent)
                                    .fillMaxHeight()
                                    .width(boxWidth)
                            ) {
                                Text(
                                    text = "Pair Device",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colorScheme.primary,
                                    modifier = Modifier
                                        .skipToLookaheadSize()
                                        .align(Alignment.CenterHorizontally)
                                )

                                Spacer(
                                    modifier = Modifier.height(10.dp)
                                )

                                Box(
                                    Modifier
                                        .height(boxHeight)
                                        .width(boxWidth)
                                        .clip(shape = RoundedCornerShape(22.dp))
                                        .border(
                                            color = colorScheme.outline,
                                            width = 0.5.dp,
                                            shape = RoundedCornerShape(22.dp)
                                        )
                                        .background(colorScheme.tertiaryContainer)
                                ) {
                                    FindDeviceScreen(
                                        viewModel,
                                        showFindDevicePanel,
                                        Modifier.matchParentSize(),
                                        { },
                                        { })
                                }
                            }
                        }
                    }
                }


            }
        }
    }

    BackHandler(showFindDevicePanel.value) {
        showFindDevicePanel.value = false
    }
}

@Composable
fun DevicesPanel(
    devices: List<BluetoothDevice>,
    connected: Boolean,
    modifier: Modifier = Modifier,
    onDeviceStatusPanelClick: () -> Unit,
    onDeviceClick: (BluetoothDevice) -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val colorScheme = MaterialTheme.colorScheme

    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = 100.dp) //Padding for AddDevicesButton
        ) {
            Column {
                Text(
                    text = "Your Devices",
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp)
                )

                Spacer(Modifier.height(24.dp))

                Row {
                    DeviceStatusPanel(
                        connected = connected,
                        onClick = onDeviceStatusPanelClick
                    )

                    DevicesListPanel(
                        devices = devices,
                        modifier = Modifier
                            .fillMaxHeight(),
                        onDeviceClick = onDeviceClick
                    )
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = 100.dp) //Padding for AddDevicesButton
        ) {
            Text(
                text = "Your Devices",
                style = MaterialTheme.typography.titleLarge,
                color = colorScheme.primary,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp)
            )

            Spacer(Modifier.height(24.dp))

            DeviceStatusPanel(
                connected = connected,
                onClick = onDeviceStatusPanelClick
            )

            Spacer(Modifier.height(20.dp))

            DevicesListPanel(
                devices = devices,
                modifier = Modifier.weight(1f),
                onDeviceClick = onDeviceClick
            )
        }
    }


}

@Composable
fun DeviceStatusPanel(
    connected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = if (isLandscape) Modifier
            .padding(start = 16.dp)
            .background(color = colorScheme.background)
        else Modifier
            .padding(horizontal = 16.dp)
            .background(color = colorScheme.background)
    ) {

        Card(
            onClick = onClick,
            enabled = connected,
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.tertiaryContainer,
                contentColor = colorScheme.surfaceContainer,
                disabledContainerColor = colorScheme.tertiaryContainer,
                disabledContentColor = colorScheme.surfaceContainer
            ),
            modifier = if (isLandscape) Modifier else Modifier.fillMaxWidth()
        ) {

            val content = @Composable {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent
                ) {
                    Icon(
                        painter = painterResource(id = Icon.SMART_GLASSES.getID()), //FIXME: change icon
                        contentDescription = null,
                        modifier = if (isLandscape) Modifier
                            .size(58.dp)
                            .align(Alignment.CenterHorizontally) else Modifier.size(58.dp)
                    )
                }

                Spacer(if (isLandscape) Modifier.height(8.dp) else Modifier.width(14.dp))

                Column(
                    modifier = if (isLandscape) Modifier else Modifier.weight(1f)
                ) {
                    var primaryText: String
                    var secondaryText: String

                    if (connected) {
                        primaryText = "Connected"
                        secondaryText = "Click here to disconnect"
                    } else {
                        primaryText = "Not Connected"
                        secondaryText = "Select a device"

                    }
                    Text(
                        text = primaryText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.primary
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = secondaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isLandscape) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    content()
                }
            } else {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    content()
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DevicesListPanel(
    devices: List<BluetoothDevice>,
    modifier: Modifier = Modifier,
    onDeviceClick: (BluetoothDevice) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.tertiaryContainer),
        ) {
            Column {
                Text(
                    text = "Paired Devices",
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = colorScheme.primary
                )

                HorizontalDivider( //todo
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    thickness = 1.dp,
                    color = Color.LightGray
                )

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                ) {
                    devices.forEach { device ->
                        if (device.address == ESP32Proxy.ESP32MAC) {
                            if (device.name != null) {
                                DeviceRow(
                                    deviceName = device.name,
                                    onDeviceClick = { onDeviceClick(device) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceRow(
    deviceName: String,
    onDeviceClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        onClick = onDeviceClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.tertiaryContainer,
            contentColor = colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.bluetooth_connected), //FIXME: change icon
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colorScheme.primary
            )

            Spacer(Modifier.width(12.dp))

            Text(
                text = deviceName,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.AddDevicesButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showFindDevicePanel: Boolean
) {

    val colorScheme = MaterialTheme.colorScheme

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(65.dp)
            .background(
                shape = CircleShape,
                color = if (!showFindDevicePanel) colorScheme.onSurface else colorScheme.tertiaryContainer
            )
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add device",
            tint = colorScheme.inversePrimary,
            modifier = Modifier
                .size(32.dp)
                .skipToLookaheadSize()
        )
    }
}