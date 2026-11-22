// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.presentation.screens

import android.content.Intent
import android.os.SystemClock
import android.provider.Settings as AndroidSettings
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager.Companion.ControlCommandIdentifiers
import me.kavishdevar.librepods.data.AirPodsBase
import me.kavishdevar.librepods.data.AirPodsModels
import me.kavishdevar.librepods.data.AirPodsPro2Lightning
import me.kavishdevar.librepods.data.AirPodsPro2USBC
import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryComponent
import me.kavishdevar.librepods.data.BatteryStatus
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.data.CaseBatteryReading
import me.kavishdevar.librepods.data.NoiseControlMode
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.presentation.theme.devicePanel
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsUiState
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import me.kavishdevar.librepods.presentation.viewmodel.demoInstance
import me.kavishdevar.librepods.presentation.viewmodel.demoState

private const val REFRESH_WINDOW_MS = 3000L

@Composable
internal fun AndpopDashboard(
    viewModel: AirPodsViewModel,
    onDeviceControl: (DashboardDeviceControl) -> Unit,
    onAppSettings: () -> Unit,
    onAccount: () -> Unit,
    onTroubleshooting: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val connected = viewModel.isReady && state.isLocallyConnected
    val snapshot = remember(state.caseBattery, connected) {
        if (connected) state.caseBattery else state.caseBattery.copy(direct = null)
    }
    val clock by produceState(SystemClock.elapsedRealtime(), snapshot) {
        do {
            value = SystemClock.elapsedRealtime()
            val expiry = snapshot.nextExpiry(value) ?: break
            delay(expiry - value)
        } while (true)
    }
    val current = snapshot.current(clock)
    var refreshing by remember { mutableStateOf(false) }
    LaunchedEffect(refreshing) {
        if (refreshing) {
            delay(REFRESH_WINDOW_MS)
            refreshing = false
        }
    }

    DashboardContent(
        state = state,
        connected = connected,
        caseReading = current ?: snapshot.lastKnown,
        caseCurrent = current != null,
        refreshing = refreshing,
        onRefresh = {
            refreshing = true
            viewModel.refreshBattery()
        },
        onOpenBluetooth = { context.startActivity(Intent(AndroidSettings.ACTION_BLUETOOTH_SETTINGS)) },
        onSelectMode = { mode ->
            viewModel.setControlCommandInt(ControlCommandIdentifiers.LISTENING_MODE, mode.ordinal + 1)
        },
        onWearDetectionChanged = viewModel::setAutomaticEarDetectionEnabled,
        onConversationAwarenessChanged = { checked ->
            viewModel.setControlCommandBoolean(ControlCommandIdentifiers.CONVERSATION_DETECT_CONFIG, checked)
        },
        onDeviceControl = onDeviceControl,
        onAppSettings = onAppSettings,
        onAccount = onAccount,
        onTroubleshooting = onTroubleshooting,
    )
}

@Composable
internal fun DashboardContent(
    state: AirPodsUiState,
    connected: Boolean,
    caseReading: CaseBatteryReading?,
    caseCurrent: Boolean,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenBluetooth: () -> Unit,
    onSelectMode: (NoiseControlMode) -> Unit,
    onWearDetectionChanged: (Boolean) -> Unit,
    onConversationAwarenessChanged: (Boolean) -> Unit,
    onDeviceControl: (DashboardDeviceControl) -> Unit,
    onAppSettings: () -> Unit,
    onTroubleshooting: () -> Unit,
    onAccount: () -> Unit = {},
) {
    val palette = LocalAndpopPalette.current
    val colors = palette.scheme
    val model = state.instance?.model
    val headphones = state.modelName.startsWith("AirPods Max") ||
        state.battery.any { it.component == BatteryComponent.HEADPHONES }
    val battery = dashboardBattery(state, connected, headphones, caseReading, caseCurrent)
    var showBattery by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(battery.isEmpty()) {
        if (battery.isEmpty()) showBattery = false
    }
    val batteryVisible = showBattery && battery.isNotEmpty()
    val blur by animateDpAsState(if (batteryVisible) 12.dp else 0.dp, label = "Battery details blur")

    Box(
        Modifier.fillMaxSize().background(colors.surface).blur(blur)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.widthIn(max = 540.dp).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.app_name),
                    Modifier.weight(1f),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onAccount) { Text("Account") }
                IconButton(
                    onClick = onAppSettings,
                    modifier = Modifier.size(48.dp),
                ) {
                    val settings = stringResource(R.string.settings)
                    DeviceArt(
                        DeviceArtKind.Settings,
                        Modifier.size(21.dp).semantics { contentDescription = settings },
                        colors.onSurface,
                    )
                }
            }

            DeviceHeader(state.deviceName, model, connected)

            if (battery.isNotEmpty()) {
                BatterySection(
                    items = battery,
                    model = model,
                    caseNeedsReading = battery.any { it.art == DeviceArtKind.Case } && !caseCurrent,
                    refreshing = refreshing,
                    onRefresh = onRefresh,
                    onQuickView = { showBattery = true },
                )
            }

            if (connected) {
                if (Capability.LISTENING_MODE in state.capabilities) {
                    DashboardListeningModes(state, onSelectMode)
                }
                DashboardDeviceSettings(
                    state = state,
                    onWearDetectionChanged = onWearDetectionChanged,
                    onConversationAwarenessChanged = onConversationAwarenessChanged,
                    onOpenControl = onDeviceControl,
                )
            } else {
                Column(
                    Modifier.fillMaxWidth().devicePanel(palette).padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DeviceArt(DeviceArtKind.Buds, Modifier.size(50.dp), colors.primary)
                    Text(
                        stringResource(R.string.andpop_pair_body),
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Button(onClick = onOpenBluetooth, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.andpop_bluetooth_settings))
                    }
                    TextButton(onClick = onTroubleshooting) {
                        Text(stringResource(R.string.troubleshooting))
                    }
                }
            }
        }
    }

    if (batteryVisible) {
        BatteryQuickView(
            deviceName = state.deviceName,
            model = model,
            items = battery,
            onDismiss = { showBattery = false },
        )
    }
}

@Composable
internal fun DashboardHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeviceHeader(deviceName: String, model: AirPodsBase?, connected: Boolean) {
    val palette = LocalAndpopPalette.current
    val colors = palette.scheme
    Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val heading = if (connected) deviceName.ifBlank { model?.displayName ?: "AirPods" }
            else stringResource(R.string.andpop_pair_title)
        Text(
            heading,
            Modifier.semantics { heading() },
            color = colors.onBackground,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (connected) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                val modelLabel = when (model) {
                    is AirPodsPro2USBC -> stringResource(R.string.andpop_pro2_usb)
                    is AirPodsPro2Lightning -> stringResource(R.string.andpop_pro2_lightning)
                    else -> model?.name
                }
                if (!modelLabel.isNullOrBlank()) {
                    Text(modelLabel, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    Modifier.background(palette.connected.copy(alpha = 0.11f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(6.dp).background(palette.connected, CircleShape))
                    Text(
                        stringResource(R.string.andpop_connected),
                        color = palette.connected,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Preview(name = "Device settings · Dark", widthDp = 411, heightDp = 850, showBackground = true, uiMode = 0x20)
@Preview(name = "Device settings · Light", widthDp = 411, heightDp = 850, showBackground = true, uiMode = 0x10)
@Composable
private fun AndpopDashboardPreview() = PreviewDashboard("A3047", "AirPods Pro 2")

@Preview(name = "AirPods Max · Dark", widthDp = 411, heightDp = 850, showBackground = true, uiMode = 0x20)
@Composable
private fun AndpopMaxDashboardPreview() = PreviewDashboard("A3184", "AirPods Max")

@Composable
private fun PreviewDashboard(modelNumber: String, name: String) {
    LibrePodsTheme(m3eEnabled = true) {
        val model = AirPodsModels.getModelByModelNumber(modelNumber)!!
        val demo = demoState.copy(
            instance = demoInstance.copy(model = model),
            deviceName = name,
            modelName = model.displayName,
            battery = if (modelNumber == "A3184") {
                listOf(Battery(BatteryComponent.HEADPHONES, 68, BatteryStatus.NOT_CHARGING))
            } else {
                demoState.battery
            },
            capabilities = model.capabilities,
            offListeningMode = true,
            isPremium = true,
            controlStates = mapOf(ControlCommandIdentifiers.LISTENING_MODE to byteArrayOf(2)),
        )
        DashboardContent(
            state = demo,
            connected = true,
            caseReading = CaseBatteryReading(1, charging = false, estimated = false, observedAt = 0),
            caseCurrent = true,
            refreshing = false,
            onRefresh = {},
            onOpenBluetooth = {},
            onSelectMode = {},
            onWearDetectionChanged = {},
            onDeviceControl = {},
            onConversationAwarenessChanged = {},
            onAppSettings = {},
            onTroubleshooting = {},
        )
    }
}
