package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.bluetooth.ChimeChannel
import com.example.data.BatteryLogEntity
import com.example.data.DeviceProfileEntity
import com.example.model.FirmwareCheckState
import com.example.model.FirmwareEvaluation
import com.example.model.ListeningMode
import com.example.model.MicrophoneMode
import com.example.model.PodFormFactor
import com.example.ui.components.AncControlBar
import com.example.ui.components.BatteryHistoryChart
import com.example.ui.components.CaseBatteryBar
import com.example.ui.components.CircularPodBatteryGauge
import com.example.ui.components.CompactHeadsetBatteryRow
import com.example.ui.components.RenameDeviceDialog
import com.example.ui.components.SignalBarsIcon
import com.example.ui.theme.BatteryMidAmber
import com.example.ui.theme.CapodMint
import com.example.viewmodel.MatchedDeviceState
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    deviceState: MatchedDeviceState?,
    batteryHistory: List<BatteryLogEntity>,
    activeChimeChannel: ChimeChannel?,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onEditProfile: (Long) -> Unit,
    onRenameDevice: (DeviceProfileEntity, String) -> Unit,
    onResetDeviceName: (DeviceProfileEntity) -> Unit,
    onOpenAnalytics: (Long) -> Unit,
    onSelectListeningMode: (ListeningMode) -> Unit,
    onSelectMicrophoneMode: (MicrophoneMode) -> Unit,
    onSetConversationalAwareness: (Boolean) -> Unit,
    onSetPersonalizedVolume: (Boolean) -> Unit,
    onSetAllowAncOnePod: (Boolean) -> Unit,
    onSetAdaptiveNoiseLevel: (Int) -> Unit,
    onCheckFirmware: (DeviceProfileEntity) -> Unit,
    onPlayChime: (ChimeChannel) -> Unit,
    onStopChime: () -> Unit,
    onClearHistory: (Long) -> Unit,
) {
    if (deviceState == null) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back)
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.status_waiting_signal),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val context = LocalContext.current
    val profile = deviceState.profile
    val model = profile.model
    val snapshot = deviceState.liveSnapshot
    var showRenameDialog by rememberSaveable(profile.id) { mutableStateOf(false) }
    var confirmClear by rememberSaveable(profile.id) { mutableStateOf(false) }
    if (confirmClear) {
        com.example.ui.components.ConfirmRemovalDialog(stringResource(R.string.settings_clear_history),
            stringResource(R.string.confirm_history_device, deviceState.displayName), stringResource(R.string.action_clear),
            onDismiss = { confirmClear = false }, onConfirm = { confirmClear = false; onClearHistory(profile.id) })
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onCheckFirmware(profile)
    }

    val triggerFirmwareCheck: () -> Unit = {
        val needsNotifPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        if (needsNotifPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onCheckFirmware(profile)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = deviceState.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.testTag("detail_device_title")
                        )
                        Text(
                            text = model.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showRenameDialog = true },
                        modifier = Modifier.testTag("detail_rename_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.cd_rename_device)
                        )
                    }
                    IconButton(
                        onClick = { onEditProfile(profile.id) },
                        modifier = Modifier.testTag("detail_edit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = stringResource(R.string.cd_edit_profile)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
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
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .testTag("detail_lazy_column"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Hero Live Battery + ANC Card
                item(key = "hero_battery") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (model.formFactor == PodFormFactor.SINGLE_HEADSET) {
                                CompactHeadsetBatteryRow(
                                    batteryPercent = deviceState.effectiveHeadsetBattery,
                                    isCharging = snapshot?.isHeadsetCharging == true,
                                    model = model
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    CircularPodBatteryGauge(
                                        batteryPercent = snapshot?.leftBattery,
                                        isCharging = snapshot?.isLeftCharging == true,
                                        isInEar = snapshot?.isLeftInEar == true,
                                        isRight = false,
                                        model = model,
                                        ringSize = 86.dp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    CircularPodBatteryGauge(
                                        batteryPercent = snapshot?.rightBattery,
                                        isCharging = snapshot?.isRightCharging == true,
                                        isInEar = snapshot?.isRightInEar == true,
                                        isRight = true,
                                        model = model,
                                        ringSize = 86.dp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                CaseBatteryBar(
                                    caseBatteryPercent = snapshot?.caseBattery,
                                    isCharging = snapshot?.isCaseCharging == true
                                )
                            }

                            if (model.supportsAnc) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                AncControlBar(
                                    selectedMode = profile.preferredListeningMode,
                                    supportsAdaptive = model.supportsAdaptive,
                                    isUpdating = deviceState.isUpdatingAnc,
                                    onSelectMode = onSelectListeningMode
                                )
                            }
                        }
                    }
                }

                // Firmware Version & Update Check Card
                item(key = "firmware_status") {
                    FirmwareStatusCard(
                        evaluation = deviceState.firmwareEvaluation,
                        isChecking = deviceState.isCheckingFirmware,
                        onCheckFirmware = triggerFirmwareCheck,
                    )
                }

                // Audio & Microphone AAP Controls Card (for Dual-Pod / Adaptive devices)
                if (model.formFactor == PodFormFactor.DUAL_POD || model.supportsAdaptive) {
                    item(key = "audio_controls") {
                        AudioAndMicControlsCard(
                            profile = profile,
                            onSelectMicrophoneMode = onSelectMicrophoneMode,
                            onSetAllowAncOnePod = onSetAllowAncOnePod,
                            onSetConversationalAwareness = onSetConversationalAwareness,
                            onSetPersonalizedVolume = onSetPersonalizedVolume,
                            onSetAdaptiveNoiseLevel = onSetAdaptiveNoiseLevel,
                        )
                    }
                }

                // Find My Pods Proximity Radar & Stereo Locating Chime Card
                item(key = "find_pods") {
                    FindMyPodsCard(
                        rssi = snapshot?.rssi,
                        signalBars = snapshot?.signalBars ?: 0,
                        formFactor = model.formFactor,
                        activeChimeChannel = activeChimeChannel,
                        onPlayChime = onPlayChime,
                        onStopChime = onStopChime
                    )
                }

                // Battery History Analytics Card
                item(key = "battery_history") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.detail_battery_history),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = { onOpenAnalytics(profile.id) },
                                        modifier = Modifier.testTag("open_full_trends_button")
                                    ) {
                                        Text(stringResource(R.string.action_open_trends))
                                    }
                                    if (batteryHistory.isNotEmpty()) {
                                        TextButton(
                                            onClick = { confirmClear = true },
                                            modifier = Modifier.testTag("clear_history_button")
                                        ) {
                                            Text(stringResource(R.string.action_clear))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            BatteryHistoryChart(
                                logs = batteryHistory,
                                formFactor = model.formFactor
                            )
                        }
                    }
                }

                // Live Telemetry & Continuity Packet Diagnostics
                item(key = "telemetry_diagnostics") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.detail_live_telemetry),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            TelemetryKeyValueRow(
                                label = "Model ID",
                                value = "0x%04X (${model.manufacturer})".format(
                                    snapshot?.modelCode ?: model.modelCode
                                )
                            )
                            TelemetryKeyValueRow(
                                label = "Bluetooth Link",
                                value = profile.linkedBluetoothAddress ?: "BLE Broadcast Only"
                            )
                            TelemetryKeyValueRow(
                                label = "Live Signal",
                                value = snapshot?.let { "${it.rssi} dBm (${it.signalBars}/4)" }
                                    ?: "Out of range"
                            )
                            TelemetryKeyValueRow(
                                label = "IRK Verification",
                                value = when {
                                    profile.identityResolvingKeyHex.isNullOrBlank() -> "Not configured"
                                    snapshot?.isIrkVerified == true -> "Verified (RPA match)"
                                    else -> "Configured"
                                }
                            )
                            if (snapshot != null) {
                                TelemetryKeyValueRow(
                                    label = "Device Finish",
                                    value = snapshot.deviceColorName
                                )
                                if (model.formFactor == PodFormFactor.DUAL_POD) {
                                    TelemetryKeyValueRow(
                                        label = "Case Lid",
                                        value = if (snapshot.isLidOpen) "Open" else "Closed"
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.label_raw_payload),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = snapshot.rawPayloadHex,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "detail_bottom_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showRenameDialog) {
        RenameDeviceDialog(
            currentName = deviceState.displayName,
            defaultName = profile.name,
            bluetoothAddress = profile.linkedBluetoothAddress,
            hasCustomName = deviceState.hasCustomName,
            onDismiss = { showRenameDialog = false },
            onConfirmRename = { newName ->
                onRenameDevice(profile, newName)
            },
            onResetToDefault = {
                onResetDeviceName(profile)
            }
        )
    }
}

@Composable
private fun FindMyPodsCard(
    rssi: Int?,
    signalBars: Int,
    formFactor: PodFormFactor,
    activeChimeChannel: ChimeChannel?,
    onPlayChime: (ChimeChannel) -> Unit,
    onStopChime: () -> Unit,
) {
    // Normalize RSSI (-95 dBm -> 0.05, -40 dBm -> 1.0)
    val proximityFraction = if (rssi != null) {
        ((rssi + 95f) / 55f).coerceIn(0.08f, 1f)
    } else {
        0f
    }
    val animatedProximity by animateFloatAsState(
        targetValue = proximityFraction,
        animationSpec = spring(),
        label = "proximity_meter"
    )
    val pulse = if (rssi != null) {
        val pulseTransition = rememberInfiniteTransition(label = "finder_pulse")
        val animatedPulse by pulseTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "finder_ring"
        )
        animatedPulse
    } else {
        0f
    }

    val proximityLabel = when {
        rssi == null -> stringResource(R.string.finder_signal_none)
        rssi >= -55 -> stringResource(R.string.finder_very_close)
        rssi >= -70 -> stringResource(R.string.finder_nearby)
        else -> stringResource(R.string.finder_far)
    }
    val proximityColor = when {
        rssi == null -> MaterialTheme.colorScheme.onSurfaceVariant
        rssi >= -55 -> CapodMint
        rssi >= -70 -> MaterialTheme.colorScheme.primary
        else -> BatteryMidAmber
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Radar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.detail_find_my_pods),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Live BLE Proximity Gauge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxR = size.minDimension / 2f
                        drawCircle(
                            color = proximityColor.copy(alpha = 0.16f),
                            radius = maxR
                        )
                        if (rssi != null) {
                            drawCircle(
                                color = proximityColor.copy(alpha = (1f - pulse) * 0.45f),
                                radius = maxR * (0.35f + 0.65f * pulse),
                                style = Stroke(width = 2.dp.toPx())
                            )
                            drawCircle(
                                color = proximityColor.copy(alpha = 0.35f),
                                radius = maxR * animatedProximity
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = proximityColor,
                        modifier = Modifier.size(16.dp)
                    ) {}
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = proximityLabel,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (rssi != null) {
                            SignalBarsIcon(bars = signalBars)
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (rssi != null) {
                            "$rssi dBm · ${(animatedProximity * 100).toInt()}% proximity"
                        } else {
                            stringResource(R.string.finder_signal_hint)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Stereo Locating Chime Controls (plays real stereo-panned AudioTrack chirp)
            Text(
                text = stringResource(R.string.finder_chime_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (activeChimeChannel != null) {
                Button(
                    onClick = onStopChime,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("stop_chime_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_stop_chime))
                }
            } else if (formFactor == PodFormFactor.SINGLE_HEADSET) {
                FilledTonalButton(
                    onClick = { onPlayChime(ChimeChannel.BOTH) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("play_chime_both_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_play_chime))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onPlayChime(ChimeChannel.LEFT) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("play_chime_left_button"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.label_left_pod), maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = { onPlayChime(ChimeChannel.BOTH) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("play_chime_both_button"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(stringResource(R.string.label_both_pods), maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = { onPlayChime(ChimeChannel.RIGHT) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("play_chime_right_button"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.label_right_pod), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun FirmwareStatusCard(
    evaluation: FirmwareEvaluation,
    isChecking: Boolean,
    onCheckFirmware: () -> Unit,
) {
    val statusColor = when (evaluation.state) {
        FirmwareCheckState.UP_TO_DATE -> CapodMint
        FirmwareCheckState.UPDATE_AVAILABLE -> BatteryMidAmber
        FirmwareCheckState.NOT_CHECKED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusLabel = when (evaluation.state) {
        FirmwareCheckState.UP_TO_DATE -> stringResource(R.string.firmware_up_to_date)
        FirmwareCheckState.UPDATE_AVAILABLE -> stringResource(R.string.firmware_update_available)
        FirmwareCheckState.NOT_CHECKED -> stringResource(R.string.firmware_not_checked)
    }
    val headerIcon = when (evaluation.state) {
        FirmwareCheckState.UP_TO_DATE -> Icons.Rounded.CheckCircle
        FirmwareCheckState.UPDATE_AVAILABLE -> Icons.Rounded.SystemUpdate
        FirmwareCheckState.NOT_CHECKED -> Icons.Rounded.Memory
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("firmware_status_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (evaluation.isUpdateAvailable) 1.5.dp else 1.dp,
            color = if (evaluation.isUpdateAvailable) {
                BatteryMidAmber.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = headerIcon,
                        contentDescription = null,
                        tint = if (evaluation.state == FirmwareCheckState.NOT_CHECKED) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            statusColor
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.detail_firmware_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.16f),
                    modifier = Modifier.testTag("firmware_status_badge")
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Side-by-side Installed vs Latest version readout tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.firmware_current_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = evaluation.currentVersion ?: "—",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("firmware_installed_version")
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.firmware_latest_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = evaluation.latestVersion,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("firmware_latest_version")
                        )
                    }
                }
            }

            when (evaluation.state) {
                FirmwareCheckState.UPDATE_AVAILABLE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BatteryMidAmber.copy(alpha = 0.10f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = evaluation.releaseHighlights,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = evaluation.updateInstructions,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FirmwareCheckState.UP_TO_DATE -> {
                    val checkedText = evaluation.lastCheckedMillis?.let { millis ->
                        val formatted = DateFormat.getDateTimeInstance(
                            DateFormat.SHORT,
                            DateFormat.SHORT
                        ).format(Date(millis))
                        "Checked $formatted · ${evaluation.releaseHighlights}"
                    } ?: evaluation.releaseHighlights
                    Text(
                        text = checkedText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FirmwareCheckState.NOT_CHECKED -> {
                    Text(
                        text = stringResource(R.string.firmware_check_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FilledTonalButton(
                onClick = onCheckFirmware,
                enabled = !isChecking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("check_firmware_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.status_checking_firmware))
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_check_firmware))
                }
            }
        }
    }
}

@Composable
private fun AudioAndMicControlsCard(
    profile: DeviceProfileEntity,
    onSelectMicrophoneMode: (MicrophoneMode) -> Unit,
    onSetAllowAncOnePod: (Boolean) -> Unit,
    onSetConversationalAwareness: (Boolean) -> Unit,
    onSetPersonalizedVolume: (Boolean) -> Unit,
    onSetAdaptiveNoiseLevel: (Int) -> Unit,
) {
    val model = profile.model
    var sliderLevel by remember(profile.id, profile.adaptiveNoiseLevel) {
        mutableFloatStateOf(profile.adaptiveNoiseLevel.toFloat())
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audio_controls_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.detail_audio_controls),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (model.formFactor == PodFormFactor.DUAL_POD) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.detail_mic_mode),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    val micOptions = listOf(
                        MicrophoneMode.AUTOMATIC to stringResource(R.string.mic_mode_auto),
                        MicrophoneMode.ALWAYS_LEFT to stringResource(R.string.mic_mode_left),
                        MicrophoneMode.ALWAYS_RIGHT to stringResource(R.string.mic_mode_right),
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        micOptions.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                selected = profile.microphoneMode == mode,
                                onClick = { onSelectMicrophoneMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = micOptions.size
                                ),
                                modifier = Modifier.testTag("mic_mode_${mode.id}")
                            ) {
                                Text(label, maxLines = 1)
                            }
                        }
                    }
                }
            }

            if (model.supportsAnc && model.formFactor == PodFormFactor.DUAL_POD) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AudioControlSwitchRow(
                    title = stringResource(R.string.detail_anc_one_pod),
                    subtitle = stringResource(R.string.detail_anc_one_pod_desc),
                    checked = profile.allowAncOnePodEnabled,
                    onCheckedChange = onSetAllowAncOnePod,
                    tag = "anc_one_pod_switch"
                )
            }

            if (model.supportsAdaptive) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AudioControlSwitchRow(
                    title = stringResource(R.string.detail_conversation_awareness),
                    subtitle = stringResource(R.string.detail_conversation_awareness_desc),
                    checked = profile.conversationalAwarenessEnabled,
                    onCheckedChange = onSetConversationalAwareness,
                    tag = "conversation_awareness_switch"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AudioControlSwitchRow(
                    title = stringResource(R.string.detail_personalized_volume),
                    subtitle = stringResource(R.string.detail_personalized_volume_desc),
                    checked = profile.personalizedVolumeEnabled,
                    onCheckedChange = onSetPersonalizedVolume,
                    tag = "personalized_volume_switch"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.detail_adaptive_audio_level),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${sliderLevel.roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = sliderLevel,
                        onValueChange = { sliderLevel = it },
                        onValueChangeFinished = {
                            onSetAdaptiveNoiseLevel(sliderLevel.roundToInt())
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.testTag("adaptive_audio_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.detail_adaptive_audio_more_noise),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.detail_adaptive_audio_less_noise),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioControlSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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

@Composable
private fun TelemetryKeyValueRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
