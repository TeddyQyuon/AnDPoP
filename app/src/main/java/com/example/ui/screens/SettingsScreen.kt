package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Widgets
import androidx.core.content.ContextCompat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.ui.components.ConfirmRemovalDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.bluetooth.EarDetectionSyncUiState
import com.example.model.AppSettings
import com.example.model.BleScanMode
import com.example.model.ThemeMode
import com.example.widget.CAPodBatteryWidgetProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    earDetectionSyncState: EarDetectionSyncUiState,
    activeBluetoothDeviceName: String?,
    snackbarHostState: SnackbarHostState,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetScanMode: (BleScanMode) -> Unit,
    onSetShowUnmatched: (Boolean) -> Unit,
    onSetAutomaticEarDetection: (Boolean) -> Unit,
    onSyncEarDetectionGatt: () -> Unit,
    onSetShowPopup: (Boolean) -> Unit,
    onSetBatteryNotification: (Boolean) -> Unit,
    onSetFirmwareAlerts: (Boolean) -> Unit,
    onNotificationPermissionDenied: () -> Unit,
    onWidgetPinResult: (Boolean) -> Unit,
    onClearAllBatteryHistory: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    if (confirmClear) {
        ConfirmRemovalDialog(stringResource(R.string.settings_clear_history),
            stringResource(R.string.settings_clear_history_desc), stringResource(R.string.action_clear),
            onDismiss = { confirmClear = false }, onConfirm = { confirmClear = false; onClearAllBatteryHistory() })
    }
    if (showPrivacy) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPrivacy = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.PrivacyTip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(stringResource(R.string.privacy_title)) },
            text = { Text(stringResource(R.string.privacy_body), modifier = Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { showPrivacy = false }) { Text(stringResource(android.R.string.ok)) } }
        )
    }
    val context = LocalContext.current
    val hasNotifPermission: () -> Boolean = {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onSetBatteryNotification(granted)
        if (!granted) onNotificationPermissionDenied()
    }
    val firmwareNotifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onSetFirmwareAlerts(granted)
        if (!granted) onNotificationPermissionDenied()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_settings),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        }
    ) { innerPadding ->
        val topGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(topGlow, Color.Transparent),
                            center = Offset(size.width * 0.85f, size.height * 0.05f),
                            radius = size.width * 0.8f,
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.05f),
                        radius = size.width * 0.8f,
                    )
                }
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Companion Features & Bluetooth GATT Controls (Featured First for Immediate Reach)
                item(key = "behavior") {
                    SettingsSectionCard(
                        sectionTitle = stringResource(R.string.settings_section_behavior),
                        sectionIcon = Icons.Rounded.Hearing,
                    ) {
                        SettingsSwitchRow(
                            icon = Icons.Rounded.Hearing,
                            title = stringResource(R.string.settings_ear_detection_title),
                            subtitle = stringResource(R.string.settings_ear_detection_desc),
                            checked = settings.automaticEarDetectionEnabled,
                            onCheckedChange = onSetAutomaticEarDetection,
                            tag = "automatic_ear_detection_switch"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        EarDetectionGattStatusBar(
                            syncState = earDetectionSyncState,
                            activeBluetoothDeviceName = activeBluetoothDeviceName,
                            onSyncNow = onSyncEarDetectionGatt,
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        SettingsSwitchRow(
                            icon = Icons.Rounded.Tune,
                            title = stringResource(R.string.settings_popup_enabled),
                            subtitle = stringResource(R.string.settings_popup_enabled_desc),
                            checked = settings.showConnectionPopup,
                            onCheckedChange = onSetShowPopup,
                            tag = "connection_popup_switch"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        SettingsSwitchRow(
                            icon = Icons.Rounded.NotificationsActive,
                            title = stringResource(R.string.settings_notification_enabled),
                            subtitle = stringResource(R.string.settings_notification_enabled_desc),
                            checked = settings.batteryNotificationEnabled,
                            onCheckedChange = { wantEnable ->
                                if (wantEnable && !hasNotifPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                } else {
                                    onSetBatteryNotification(wantEnable)
                                }
                            },
                            tag = "battery_notification_switch"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        SettingsSwitchRow(
                            icon = Icons.Rounded.SystemUpdate,
                            title = stringResource(R.string.settings_firmware_alerts),
                            subtitle = stringResource(R.string.settings_firmware_alerts_desc),
                            checked = settings.firmwareAlertsEnabled,
                            onCheckedChange = { wantEnable ->
                                if (wantEnable && !hasNotifPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    firmwareNotifPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                } else {
                                    onSetFirmwareAlerts(wantEnable)
                                }
                            },
                            tag = "firmware_alerts_switch"
                        )
                    }
                }

                // Appearance Section
                item(key = "appearance") {
                    SettingsSectionCard(
                        sectionTitle = stringResource(R.string.settings_section_appearance),
                        sectionIcon = Icons.Rounded.Palette,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_theme),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val themeOptions = listOf(
                            ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                            ThemeMode.LIGHT to stringResource(R.string.theme_light),
                            ThemeMode.DARK to stringResource(R.string.theme_dark),
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            themeOptions.forEachIndexed { index, (mode, label) ->
                                SegmentedButton(
                                    selected = settings.themeMode == mode,
                                    onClick = { onSetThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = themeOptions.size
                                    ),
                                    modifier = Modifier.testTag("theme_option_${mode.key}")
                                ) {
                                    Text(label, maxLines = 1)
                                }
                            }
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            SettingsSwitchRow(
                                icon = Icons.Rounded.Palette,
                                title = stringResource(R.string.settings_dynamic_color),
                                subtitle = stringResource(R.string.settings_dynamic_color_desc),
                                checked = settings.dynamicColor,
                                onCheckedChange = onSetDynamicColor,
                                tag = "dynamic_color_switch"
                            )
                        }
                    }
                }

                // Bluetooth Scanner Section
                item(key = "scanner") {
                    SettingsSectionCard(
                        sectionTitle = stringResource(R.string.settings_section_scanner),
                        sectionIcon = Icons.AutoMirrored.Rounded.BluetoothSearching,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_scan_mode),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val scanOptions = listOf(
                            BleScanMode.LOW_LATENCY to stringResource(R.string.scan_mode_low_latency),
                            BleScanMode.BALANCED to stringResource(R.string.scan_mode_balanced),
                            BleScanMode.LOW_POWER to stringResource(R.string.scan_mode_low_power),
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            scanOptions.forEachIndexed { index, (mode, label) ->
                                SegmentedButton(
                                    selected = settings.scanMode == mode,
                                    onClick = { onSetScanMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = scanOptions.size
                                    ),
                                    modifier = Modifier.testTag("scan_mode_${mode.key}")
                                ) {
                                    Text(label, maxLines = 1)
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        SettingsSwitchRow(
                            icon = Icons.Rounded.Visibility,
                            title = stringResource(R.string.settings_show_unmatched),
                            subtitle = stringResource(R.string.settings_show_unmatched_desc),
                            checked = settings.showUnmatchedDevices,
                            onCheckedChange = onSetShowUnmatched,
                            tag = "show_unmatched_switch"
                        )
                    }
                }

                // Widgets & Data Section
                item(key = "widgets_and_data") {
                    SettingsSectionCard(
                        sectionTitle = stringResource(R.string.settings_section_widgets),
                        sectionIcon = Icons.Rounded.Widgets,
                    ) {
                        if (CAPodBatteryWidgetProvider.canRequestPinWidget(context)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.settings_pin_widget),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_pin_widget_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalButton(
                                    onClick = {
                                        val requested = CAPodBatteryWidgetProvider.requestPinWidget(context)
                                        onWidgetPinResult(requested)
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("pin_widget_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(stringResource(R.string.action_pin_widget))
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_clear_history),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.settings_clear_history_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = { confirmClear = true },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("clear_all_history_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.action_clear))
                            }
                        }
                    }
                }

                // About Section
                item(key = "about") {
                    SettingsSectionCard(
                        sectionTitle = stringResource(R.string.settings_section_about),
                        sectionIcon = Icons.Rounded.Info,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.settings_protocol_info),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_version, com.example.BuildConfig.VERSION_NAME),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showPrivacy = true }
                                .padding(vertical = 4.dp)
                                .testTag("settings_privacy_policy_row"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PrivacyTip,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.privacy_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.label_privacy_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EarDetectionGattStatusBar(
    syncState: EarDetectionSyncUiState,
    activeBluetoothDeviceName: String?,
    onSyncNow: () -> Unit,
) {
    val isSyncing = syncState is EarDetectionSyncUiState.Syncing
    val isSynced = syncState is EarDetectionSyncUiState.SyncedGatt ||
        syncState is EarDetectionSyncUiState.SyncedAap

    val containerColor = when {
        isSynced -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        isSyncing -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = when {
        isSynced -> MaterialTheme.colorScheme.onSecondaryContainer
        isSyncing -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusText = when (syncState) {
        is EarDetectionSyncUiState.Syncing ->
            stringResource(R.string.ear_detection_gatt_syncing)
        is EarDetectionSyncUiState.SyncedGatt ->
            stringResource(R.string.ear_detection_gatt_synced, syncState.deviceName)
        is EarDetectionSyncUiState.SyncedAap ->
            stringResource(R.string.ear_detection_aap_synced, syncState.deviceName)
        is EarDetectionSyncUiState.SavedLocally ->
            stringResource(R.string.ear_detection_gatt_local_only, syncState.reason)
        EarDetectionSyncUiState.Idle -> {
            if (!activeBluetoothDeviceName.isNullOrBlank()) {
                stringResource(R.string.ear_detection_gatt_ready, activeBluetoothDeviceName)
            } else {
                stringResource(R.string.ear_detection_gatt_idle)
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ear_detection_gatt_status"),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            } else {
                Icon(
                    imageVector = if (isSynced) {
                        Icons.Rounded.CheckCircle
                    } else {
                        Icons.Rounded.BluetoothConnected
                    },
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onSyncNow,
                enabled = !isSyncing,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("ear_detection_sync_button"),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.action_sync_gatt),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    sectionTitle: String,
    sectionIcon: ImageVector,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = sectionIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = sectionTitle,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (checked) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(19.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}
