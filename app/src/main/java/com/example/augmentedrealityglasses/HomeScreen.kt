package com.example.augmentedrealityglasses

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.augmentedrealityglasses.ble.ESP32Proxy
import com.example.augmentedrealityglasses.ble.screens.FindDeviceScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    //TODO: delete this parameter and also his ScreenName value?
    onNavigateFindDevice: () -> Unit
) {
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

    var showFindDevicePanel by remember { mutableStateOf(false) }


    //FIXME: allow users to refresh data?
    LaunchedEffect(Unit) {
        viewModel.refreshBondedDevices(context, adapter)
    }
    ErrorWrapper(
        message = viewModel.errorMessage,
        onDismiss = { viewModel.hideErrorMessage() }
    ) {


        val modifier = if (showFindDevicePanel) Modifier
            .background(Color.Transparent)
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (showFindDevicePanel) {
                    showFindDevicePanel = false
                }
            } else Modifier.fillMaxSize()

        Box(
            modifier = modifier
        ) {

            Box(
                modifier = if (showFindDevicePanel) {
                    Modifier
                        .fillMaxSize()
                        .blur(20.dp)
                } else {
                    Modifier.fillMaxSize()
                }
            ) {

                Column {
                    Text(
                        text = "Home",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp)
                    )
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
                val boxWidth = screenWidthDp * 0.9f

                AnimatedContent(targetState = showFindDevicePanel) { targetState ->
                    if (!targetState) {

                        Box(
                            Modifier
                                .offset(
                                    x = (screenWidthDp - 64.dp) / 2,
                                    y = screenHeightDp - 155.dp //todo fix with navBarHeight
                                )
                                .sharedBounds(
                                    sharedContentState = sharedState,
                                    animatedVisibilityScope = this@AnimatedContent,
                                    resizeMode = ResizeMode.ScaleToBounds(),
                                )
                                .padding(bottom = 13.dp)

                        ) {
                            AddDevicesButton(
                                { showFindDevicePanel = true },
                                modifier = Modifier, showFindDevicePanel
                            )
                        }
                    } else {

                        Box(
                            Modifier
                                .offset(
                                    x = (screenWidthDp - boxWidth) / 2,
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
                                .height(boxHeight + 35.dp)

                        ) {
                            Column(
                                Modifier
                                    .background(Color.Transparent)
                                    .fillMaxHeight()
                                    .width(boxWidth)
                            ) {
                                Text(
                                    text = "Pair Device",
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold,
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
                                        .background(Color.White)
                                        .border(
                                            0.5.dp,
                                            Color.Black,
                                            shape = RoundedCornerShape(22.dp)
                                        )

                                ) {
                                    FindDeviceScreen(
                                        viewModel,
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
}

@Composable
fun DevicesPanel(
    devices: List<BluetoothDevice>,
    connected: Boolean,
    modifier: Modifier = Modifier,
    onDeviceStatusPanelClick: () -> Unit,
    onDeviceClick: (BluetoothDevice) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 70.dp) //Padding for AddDevicesButton
    ) {
        Text(
            text = "Your Devices",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier
                .padding(bottom = 8.dp)
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp)
        )

        DeviceStatusPanel(
            connected = connected,
            onClick = onDeviceStatusPanelClick
        )

        DevicesListPanel(
            devices = devices,
            modifier = Modifier.weight(1f),
            onDeviceClick = onDeviceClick
        )
    }
}

@Composable
fun DeviceStatusPanel(
    connected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)
    ) {
        Card(
            onClick = onClick,
            enabled = connected,
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp,
                disabledElevation = 1.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = Color.White,
                disabledContentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.eyeglasses), //FIXME: change icon
                        contentDescription = null,
                        modifier = Modifier.size(58.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    if (connected) {
                        Text(
                            text = "Currently connected",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            lineHeight = MaterialTheme.typography.titleLarge.lineHeight,
                        )

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        Text(
                            text = "Click here to disconnect",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "Device not connected",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        )
                    }
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
    Box(
        modifier = modifier.padding(13.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column {
                Text(
                    text = "Previously connected devices",
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black
                )

                HorizontalDivider(
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
    Card(
        onClick = onDeviceClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 4.dp
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
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(12.dp))

            Text(
                text = deviceName,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
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

    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = if (!showFindDevicePanel) Color.Black else Color.White,
        modifier = modifier.size(64.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add device",
            tint = Color.White,
            modifier = Modifier
                .size(32.dp)
                .skipToLookaheadSize()
        )
    }
}