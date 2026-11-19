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
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DeviceProfileEntity
import com.example.model.BlePodSnapshot
import com.example.model.BluetoothHardwareState
import com.example.model.BondedBluetoothDevice
import com.example.model.ListeningMode
import com.example.model.PodModel
import com.example.ui.components.ConnectionPopupSheet
import com.example.ui.components.DeviceModelIcon
import com.example.ui.components.MatchedDeviceCard
import com.example.ui.components.RenameDeviceDialog
import com.example.ui.components.UnmatchedDevicesCard
import com.example.ui.theme.BatteryLowRed
import com.example.ui.theme.BatteryMidAmber
import com.example.ui.theme.CapodMint
import com.example.viewmodel.CAPodUiState
import com.example.viewmodel.MatchedDeviceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    uiState: CAPodUiState,
    snackbarHostState: SnackbarHostState,
    onRequestPermissions: () -> Unit,
    onRequestEnableBluetooth: () -> Unit,
    onBluetoothUnavailable: () -> Unit,
    onToggleScanning: () -> Unit,
    onOpenDiscovery: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddProfile: () -> Unit,
    onOpenDeviceDetail: (Long) -> Unit,
    onEditProfile: (Long) -> Unit,
    onRenameDevice: (DeviceProfileEntity, String) -> Unit,
    onResetDeviceName: (DeviceProfileEntity) -> Unit,
    onQuickToggleAncMode: (DeviceProfileEntity) -> Unit,
    onSelectListeningMode: (DeviceProfileEntity, ListeningMode) -> Unit,
    onPinAncQuickTile: (MatchedDeviceState) -> Unit,
    onToggleUnmatchedExpanded: () -> Unit,
    onQuickAddUnmatched: (BlePodSnapshot) -> Unit,
    onQuickAddBonded: (BondedBluetoothDevice) -> Unit,
    onInspectUnmatched: () -> Unit,
    onDismissPopup: () -> Unit,
) {
    var renamingDevice by remember { mutableStateOf<MatchedDeviceState?>(null) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            val (statusRes, dotColor) = when (uiState.hardwareState) {
                                BluetoothHardwareState.PERMISSION_REQUIRED ->
                                    R.string.overview_chip_perm_needed to BatteryMidAmber
                                BluetoothHardwareState.BLUETOOTH_DISABLED ->
                                    R.string.overview_chip_bt_off to BatteryLowRed
                                BluetoothHardwareState.UNAVAILABLE ->
                                    R.string.bt_unavailable_title to MaterialTheme.colorScheme.outline
                                BluetoothHardwareState.READY -> if (uiState.isScanning) {
                                    R.string.overview_chip_scanning to CapodMint
                                } else {
                                    R.string.overview_chip_paused to BatteryMidAmber
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = dotColor.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, dotColor.copy(alpha = 0.28f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                    Text(
                                        text = stringResource(statusRes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    if (uiState.matchedDevices.isNotEmpty()) {
                        IconButton(
                            onClick = onAddProfile,
                            modifier = Modifier.testTag("top_add_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.cd_add_profile),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            when (uiState.hardwareState) {
                                BluetoothHardwareState.PERMISSION_REQUIRED -> onRequestPermissions()
                                BluetoothHardwareState.BLUETOOTH_DISABLED -> onRequestEnableBluetooth()
                                BluetoothHardwareState.READY -> onToggleScanning()
                                BluetoothHardwareState.UNAVAILABLE -> onBluetoothUnavailable()
                            }
                        },
                        modifier = Modifier.testTag("top_scan_toggle_button")
                    ) {
                        Icon(
                            imageVector = when {
                                uiState.hardwareState == BluetoothHardwareState.BLUETOOTH_DISABLED ||
                                    uiState.hardwareState == BluetoothHardwareState.UNAVAILABLE ->
                                    Icons.Rounded.BluetoothDisabled
                                uiState.isScanning -> Icons.AutoMirrored.Rounded.BluetoothSearching
                                else -> Icons.Rounded.Bluetooth
                            },
                            contentDescription = stringResource(R.string.cd_toggle_scan),
                            tint = if (uiState.isScanning) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("top_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.cd_open_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .widthIn(max = 640.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OverviewShortcut(
                                label = R.string.overview_quick_profiles,
                                icon = Icons.Outlined.DevicesOther,
                                tag = "top_devices_button",
                                onClick = onOpenDevices,
                                modifier = Modifier.weight(1f)
                            )
                            OverviewShortcut(
                                label = R.string.overview_quick_discover,
                                icon = Icons.Rounded.Radar,
                                tag = "top_discover_ble_button",
                                onClick = onOpenDiscovery,
                                modifier = Modifier.weight(1f)
                            )
                            OverviewShortcut(
                                label = R.string.overview_quick_trends,
                                icon = Icons.Rounded.Insights,
                                tag = "top_analytics_button",
                                onClick = onOpenAnalytics,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val primaryGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.085f)
        val secondaryGlow = MaterialTheme.colorScheme.secondary.copy(alpha = 0.065f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(primaryGlow, Color.Transparent),
                            center = Offset(size.width * 0.18f, size.height * 0.06f),
                            radius = size.width * 0.85f,
                        ),
                        center = Offset(size.width * 0.18f, size.height * 0.06f),
                        radius = size.width * 0.85f,
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(secondaryGlow, Color.Transparent),
                            center = Offset(size.width * 0.88f, size.height * 0.18f),
                            radius = size.width * 0.72f,
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.18f),
                        radius = size.width * 0.72f,
                    )
                }
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {


                // Hardware & permission status banner
                when (uiState.hardwareState) {
                    BluetoothHardwareState.PERMISSION_REQUIRED -> {
                        item(key = "perm_banner") {
                            ActionStatusBanner(
                                icon = Icons.Rounded.Bluetooth,
                                title = stringResource(R.string.perm_title),
                                description = stringResource(R.string.perm_description),
                                actionLabel = stringResource(R.string.perm_grant_button),
                                actionTag = "grant_bluetooth_button",
                                onAction = onRequestPermissions
                            )
                        }
                    }

                    BluetoothHardwareState.BLUETOOTH_DISABLED -> {
                        item(key = "bt_off_banner") {
                            ActionStatusBanner(
                                icon = Icons.Rounded.BluetoothDisabled,
                                title = stringResource(R.string.bt_off_title),
                                description = stringResource(R.string.bt_off_description),
                                actionLabel = stringResource(R.string.bt_enable_button),
                                actionTag = "enable_bluetooth_button",
                                onAction = onRequestEnableBluetooth
                            )
                        }
                    }

                    BluetoothHardwareState.UNAVAILABLE -> {
                        item(key = "bt_unavailable_banner") {
                            InfoStatusBanner(
                                title = stringResource(R.string.bt_unavailable_title),
                                description = stringResource(R.string.bt_unavailable_description)
                            )
                        }
                    }

                    BluetoothHardwareState.READY -> {
                        if (uiState.isScanPausedByUser) {
                            item(key = "scan_paused_banner") {
                                PausedScanBanner(onResume = onToggleScanning)
                            }
                        }
                    }
                }

                // Matched Device Cards
                if (uiState.matchedDevices.isNotEmpty()) {
                    items(
                        items = uiState.matchedDevices,
                        key = { "profile_${it.profile.id}" }
                    ) { matched ->
                        MatchedDeviceCard(
                            state = matched,
                            isAncQuickTileLinked = matched.profile.id == uiState.ancQuickTileProfileId,
                            onOpenDetail = { onOpenDeviceDetail(matched.profile.id) },
                            onRenameDevice = { renamingDevice = matched },
                            onEditProfile = { onEditProfile(matched.profile.id) },
                            onQuickToggleAncMode = { onQuickToggleAncMode(matched.profile) },
                            onSelectListeningMode = { mode ->
                                onSelectListeningMode(matched.profile, mode)
                            },
                            onPinAncQuickTile = { onPinAncQuickTile(matched) },
                        )
                    }
                } else {
                    item(key = "empty_radar_card") {
                        ScanningEmptyStateCard(
                            isScanning = uiState.isScanning,
                            bondedDevices = uiState.bondedDevices,
                            onAddProfile = onAddProfile,
                            onOpenDiscovery = onOpenDiscovery,
                            onQuickAddBonded = onQuickAddBonded
                        )
                    }
                }

                // Unmatched Nearby Apple Devices Card
                if (uiState.settings.showUnmatchedDevices && uiState.unmatchedDevices.isNotEmpty()) {
                    item(key = "unmatched_card") {
                        UnmatchedDevicesCard(
                            unmatchedDevices = uiState.unmatchedDevices,
                            isExpanded = uiState.isUnmatchedExpanded,
                            onToggleExpand = onToggleUnmatchedExpanded,
                            onQuickAddProfile = onQuickAddUnmatched,
                            onInspectAll = onInspectUnmatched
                        )
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Optional Case-Open Connection Popup Sheet
    val popup = uiState.popupDevice
    if (popup != null) {
        ConnectionPopupSheet(
            deviceState = popup,
            onDismiss = onDismissPopup,
            onEditProfile = { onOpenDeviceDetail(popup.profile.id) }
        )
    }

    val deviceToRename = renamingDevice
    if (deviceToRename != null) {
        RenameDeviceDialog(
            currentName = deviceToRename.displayName,
            defaultName = deviceToRename.profile.name,
            bluetoothAddress = deviceToRename.profile.linkedBluetoothAddress,
            hasCustomName = deviceToRename.hasCustomName,
            onDismiss = { renamingDevice = null },
            onConfirmRename = { updated ->
                onRenameDevice(deviceToRename.profile, updated)
            },
            onResetToDefault = {
                onResetDeviceName(deviceToRename.profile)
            }
        )
    }
}

@Composable
private fun OverviewShortcut(
    label: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(onClick = onClick, modifier = modifier.testTag(tag)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ActionStatusBanner(
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
                Text(text = actionLabel)
            }
        }
    }
}

@Composable
private fun InfoStatusBanner(
    title: String,
    description: String,
) {
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PausedScanBanner(onResume: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.scan_paused_banner),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = onResume,
                modifier = Modifier.testTag("resume_scan_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.scan_resume_button))
            }
        }
    }
}

@Composable
private fun ScanningEmptyStateCard(
    isScanning: Boolean,
    bondedDevices: List<BondedBluetoothDevice>,
    onAddProfile: () -> Unit,
    onOpenDiscovery: () -> Unit,
    onQuickAddBonded: (BondedBluetoothDevice) -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "radar_wave")
    val waveProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_ring"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(116.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxRadius = size.minDimension / 2f
                    if (isScanning) {
                        for (ring in 0..1) {
                            val phase = (waveProgress + ring * 0.5f) % 1f
                            val radius = maxRadius * (0.35f + 0.65f * phase)
                            val alpha = (1f - phase) * 0.32f
                            drawCircle(
                                color = primaryColor.copy(alpha = alpha),
                                radius = radius,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    } else {
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.14f),
                            radius = maxRadius * 0.78f,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        DeviceModelIcon(
                            model = PodModel.AIRPODS_PRO_2,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            ventTint = MaterialTheme.colorScheme.primaryContainer,
                            iconSize = 36.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(if (isScanning) R.string.status_scanning_title else R.string.empty_profiles_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.status_scanning_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onAddProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("empty_add_profile_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_add_first_profile))
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onOpenDiscovery,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("empty_discover_ble_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Radar,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_discover_ble))
            }

            // If real paired Bluetooth devices exist on this phone, let user quick-add them in 1 tap
            if (bondedDevices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.section_paired_bluetooth),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                bondedDevices.take(3).forEach { device ->
                    FilledTonalButton(
                        onClick = { onQuickAddBonded(device) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .height(48.dp)
                            .testTag("quick_add_bonded_${device.address}"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bluetooth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add ${device.displayName}",
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
