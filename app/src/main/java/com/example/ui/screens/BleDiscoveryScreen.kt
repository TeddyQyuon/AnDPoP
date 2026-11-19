package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BleDiscoveryFilter
import com.example.model.BluetoothHardwareState
import com.example.model.DiscoveredBleDevice
import com.example.ui.components.DeviceModelIcon
import com.example.ui.components.SignalBarsIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleDiscoveryScreen(
    hardwareState: BluetoothHardwareState,
    isDiscovering: Boolean,
    discoveredDevices: List<DiscoveredBleDevice>,
    snackbarHostState: SnackbarHostState,
    onRequestPermissions: () -> Unit,
    onRequestEnableBluetooth: () -> Unit,
    onStartDiscovery: (Boolean) -> Unit,
    onStopDiscovery: () -> Unit,
    onPairDevice: (DiscoveredBleDevice) -> Unit,
    onAddProfileForDevice: (DiscoveredBleDevice) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var selectedFilter by remember { mutableStateOf(BleDiscoveryFilter.ALL) }

    val filteredDevices = remember(discoveredDevices, selectedFilter) {
        when (selectedFilter) {
            BleDiscoveryFilter.ALL -> discoveredDevices
            BleDiscoveryFilter.NAMED_ONLY -> discoveredDevices.filter { it.hasName }
            BleDiscoveryFilter.APPLE_AND_AUDIO -> discoveredDevices.filter {
                it.detectedPodModel != null ||
                    it.manufacturerLabel.equals("Apple", ignoreCase = true) ||
                    it.manufacturerLabel.equals("Sony", ignoreCase = true) ||
                    it.manufacturerLabel.equals("Bose", ignoreCase = true) ||
                    it.manufacturerLabel.equals("Samsung", ignoreCase = true) ||
                    it.manufacturerLabel.equals("Google", ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.title_ble_discovery),
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (discoveredDevices.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${filteredDevices.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("discovery_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back)
                            )
                        }
                    },
                    actions = {
                        if (hardwareState == BluetoothHardwareState.READY) {
                            IconButton(
                                onClick = { onStartDiscovery(true) },
                                modifier = Modifier.testTag("discovery_refresh_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = stringResource(R.string.cd_refresh_scan),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (isDiscovering) onStopDiscovery() else onStartDiscovery(false)
                                },
                                modifier = Modifier.testTag("discovery_toggle_scan_button")
                            ) {
                                Icon(
                                    imageVector = if (isDiscovering) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isDiscovering) {
                                        stringResource(R.string.action_stop_scan)
                                    } else {
                                        stringResource(R.string.action_start_scan)
                                    },
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                if (isDiscovering) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hardware / permission guard
                when (hardwareState) {
                    BluetoothHardwareState.PERMISSION_REQUIRED -> {
                        item(key = "discovery_perm") {
                            DiscoveryGuardCard(
                                icon = Icons.Rounded.Bluetooth,
                                title = stringResource(R.string.perm_title),
                                description = stringResource(R.string.discovery_perm_desc),
                                actionLabel = stringResource(R.string.perm_grant_button),
                                actionTag = "discovery_grant_perm_button",
                                onAction = onRequestPermissions
                            )
                        }
                    }

                    BluetoothHardwareState.BLUETOOTH_DISABLED -> {
                        item(key = "discovery_bt_off") {
                            DiscoveryGuardCard(
                                icon = Icons.Rounded.BluetoothDisabled,
                                title = stringResource(R.string.bt_off_title),
                                description = stringResource(R.string.bt_off_description),
                                actionLabel = stringResource(R.string.bt_enable_button),
                                actionTag = "discovery_enable_bt_button",
                                onAction = onRequestEnableBluetooth
                            )
                        }
                    }

                    BluetoothHardwareState.UNAVAILABLE -> {
                        item(key = "discovery_unavailable") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = stringResource(R.string.bt_unavailable_title),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = stringResource(R.string.bt_unavailable_description),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    BluetoothHardwareState.READY -> {
                        // Filter chips
                        item(key = "filter_chips") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = selectedFilter == BleDiscoveryFilter.ALL,
                                    onClick = { selectedFilter = BleDiscoveryFilter.ALL },
                                    label = { Text(stringResource(R.string.filter_all)) },
                                    modifier = Modifier.testTag("filter_all_chip")
                                )
                                FilterChip(
                                    selected = selectedFilter == BleDiscoveryFilter.NAMED_ONLY,
                                    onClick = { selectedFilter = BleDiscoveryFilter.NAMED_ONLY },
                                    label = { Text(stringResource(R.string.filter_named)) },
                                    modifier = Modifier.testTag("filter_named_chip")
                                )
                                FilterChip(
                                    selected = selectedFilter == BleDiscoveryFilter.APPLE_AND_AUDIO,
                                    onClick = { selectedFilter = BleDiscoveryFilter.APPLE_AND_AUDIO },
                                    label = { Text(stringResource(R.string.filter_audio_apple)) },
                                    modifier = Modifier.testTag("filter_audio_chip")
                                )
                            }
                        }

                        if (filteredDevices.isEmpty()) {
                            item(key = "discovery_empty") {
                                DiscoveryEmptyCard(
                                    isDiscovering = isDiscovering,
                                    onToggleScan = {
                                        if (isDiscovering) onStopDiscovery() else onStartDiscovery(false)
                                    }
                                )
                            }
                        } else {
                            items(
                                items = filteredDevices,
                                key = { it.address }
                            ) { device ->
                                DiscoveredBleDeviceCard(
                                    device = device,
                                    onPair = { onPairDevice(device) },
                                    onAddProfile = { onAddProfileForDevice(device) }
                                )
                            }
                        }
                    }
                }

                item(key = "discovery_bottom_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun DiscoveredBleDeviceCard(
    device: DiscoveredBleDevice,
    onPair: () -> Unit,
    onAddProfile: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("discovered_device_${device.address}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (device.isBonded) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left icon badge
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (device.isBonded) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (device.detectedPodModel != null) {
                        DeviceModelIcon(
                            model = device.detectedPodModel,
                            tint = MaterialTheme.colorScheme.onSurface,
                            ventTint = MaterialTheme.colorScheme.surfaceContainer,
                            iconSize = 26.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Bluetooth,
                            contentDescription = null,
                            tint = if (device.isBonded) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Device identity & RSSI
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = device.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (device.hasName) FontWeight.Bold else FontWeight.Medium,
                            fontStyle = if (device.hasName) FontStyle.Normal else FontStyle.Italic
                        ),
                        color = if (device.hasName) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    SignalBarsIcon(bars = device.signalBars)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = buildString {
                        append(device.address)
                        append(" · ")
                        append("${device.rssi} dBm")
                        device.manufacturerLabel?.let {
                            append(" · ")
                            append(it)
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right pairing / status action
            when {
                device.isBonded -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = stringResource(R.string.status_paired_only),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = stringResource(R.string.status_paired_only),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        IconButton(
                            onClick = onAddProfile,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("add_profile_from_ble_${device.address}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.action_create_profile),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                device.isBonding -> {
                    FilledTonalButton(
                        onClick = {},
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("pairing_progress_${device.address}")
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.status_pairing))
                    }
                }

                else -> {
                    Button(
                        onClick = onPair,
                        enabled = device.isConnectable,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("pair_device_${device.address}")
                    ) {
                        Text(
                            text = if (device.isConnectable) {
                                stringResource(R.string.action_pair)
                            } else {
                                stringResource(R.string.label_beacon)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoveryEmptyCard(
    isDiscovering: Boolean,
    onToggleScan: () -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "ble_discovery_radar")
    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ble_wave"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(104.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxR = size.minDimension / 2f
                    if (isDiscovering) {
                        for (ring in 0..1) {
                            val phase = (wave + ring * 0.5f) % 1f
                            drawCircle(
                                color = primaryColor.copy(alpha = (1f - phase) * 0.35f),
                                radius = maxR * (0.35f + 0.65f * phase),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    } else {
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.16f),
                            radius = maxR * 0.8f,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(58.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isDiscovering) {
                                Icons.Rounded.BluetoothSearching
                            } else {
                                Icons.Rounded.Bluetooth
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isDiscovering) {
                    stringResource(R.string.discovery_scanning_title)
                } else {
                    stringResource(R.string.discovery_paused_title)
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.discovery_scanning_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            FilledTonalButton(
                onClick = onToggleScan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("discovery_primary_scan_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = if (isDiscovering) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isDiscovering) {
                        stringResource(R.string.action_stop_scan)
                    } else {
                        stringResource(R.string.action_start_scan)
                    }
                )
            }
        }
    }
}

@Composable
private fun DiscoveryGuardCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    actionLabel: String,
    actionTag: String,
    onAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag(actionTag),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}
