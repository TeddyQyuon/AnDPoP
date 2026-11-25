// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.presentation.screens

import android.content.Intent
import android.os.SystemClock
import android.provider.Settings as AndroidSettings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import me.kavishdevar.librepods.presentation.components.InfoBanner
import me.kavishdevar.librepods.presentation.components.PillTone
import me.kavishdevar.librepods.presentation.components.StatusPill
import me.kavishdevar.librepods.presentation.theme.AndpopIcons
import me.kavishdevar.librepods.presentation.theme.AndpopType
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsUiState
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import me.kavishdevar.librepods.presentation.viewmodel.demoInstance
import me.kavishdevar.librepods.presentation.viewmodel.demoState

private const val REFRESH_WINDOW_MS = 3000L
private const val RECONNECT_WINDOW_MS = 5000L

@Composable
internal fun AndpopDashboard(
    viewModel: AirPodsViewModel,
    onDeviceControl: (DashboardDeviceControl) -> Unit,
    onAppSettings: () -> Unit,
    onAccount: () -> Unit,
    onTroubleshooting: () -> Unit,
    accountInitials: String? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val loading = !viewModel.isReady && !state.isDemoMode
    val connected = (viewModel.isReady || state.isDemoMode) && state.isLocallyConnected
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
        loading = loading,
        accountInitials = accountInitials,
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
        onReconnect = if (viewModel.isReady && state.connectionSuccessful) viewModel::reconnectFromSavedMac else null,
        onTryDemo = { viewModel.setDemoMode(true) },
        onExitDemo = { viewModel.setDemoMode(false) },
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
    onReconnect: (() -> Unit)? = null,
    loading: Boolean = false,
    accountInitials: String? = null,
    onTryDemo: (() -> Unit)? = null,
    onExitDemo: (() -> Unit)? = null,
) {
    val palette = LocalAndpopPalette.current
    val model = state.instance?.model
    val headphones = state.modelName.startsWith("AirPods Max") ||
        state.battery.any { it.component == BatteryComponent.HEADPHONES }
    val battery = dashboardBattery(state, connected, headphones, caseReading, caseCurrent)
    var showBattery by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(battery.isEmpty()) {
        if (battery.isEmpty()) showBattery = false
    }
    var reconnecting by remember { mutableStateOf(false) }
    LaunchedEffect(reconnecting, connected) {
        if (connected) reconnecting = false
        if (reconnecting) {
            delay(RECONNECT_WINDOW_MS)
            reconnecting = false
        }
    }
    val batteryVisible = showBattery && battery.isNotEmpty()
    val blur by animateDpAsState(if (batteryVisible) 12.dp else 0.dp, label = "Battery details blur")
    val glow = palette.glow

    Box(
        Modifier.fillMaxSize().background(palette.bg)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(glow.copy(alpha = 0.75f), Color.Transparent),
                        center = Offset(size.width / 2f, -size.height * 0.06f),
                        radius = size.width * 1.05f,
                    ),
                )
            }
            .blur(blur)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        ) {
            DashboardTopBar(onAccount, onAppSettings, accountInitials)

            if (state.isDemoMode && onExitDemo != null) {
                InfoBanner(
                    text = stringResource(R.string.andpop_demo_banner),
                    modifier = Modifier.padding(bottom = 12.dp),
                    icon = AndpopIcons.Sparkle,
                ) {
                    TextButton(onClick = onExitDemo) {
                        Text(stringResource(R.string.andpop_exit_demo), color = palette.pri, style = AndpopType.label)
                    }
                }
            }

            when {
                loading -> DashboardLoadingSkeleton()
                connected || battery.isNotEmpty() || state.connectionSuccessful -> {
                    HeroCard(
                        title = if (connected) state.deviceName.ifBlank { model?.displayName ?: stringResource(R.string.andpop_airpods_fallback) }
                            else state.deviceName.ifBlank { stringResource(R.string.andpop_pair_title) },
                        model = model,
                        status = when {
                            connected -> Triple(stringResource(R.string.andpop_connected), PillTone.Success, false)
                            reconnecting -> Triple(stringResource(R.string.andpop_connecting), PillTone.Neutral, true)
                            else -> Triple(stringResource(R.string.andpop_not_connected), PillTone.Neutral, false)
                        },
                    ) {
                        if (battery.isNotEmpty()) {
                            BatterySection(
                                items = battery,
                                model = model,
                                caseNeedsReading = connected && battery.any { it.art == DeviceArtKind.Case } && !caseCurrent,
                                refreshing = refreshing,
                                onRefresh = onRefresh,
                                onQuickView = { showBattery = true },
                            )
                        } else {
                            Spacer(Modifier.height(12.dp))
                        }
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
                        DisconnectedActions(
                            reconnecting = reconnecting,
                            showLastReadingNote = battery.isNotEmpty(),
                            onReconnect = onReconnect?.let { reconnect -> { reconnecting = true; reconnect() } },
                            onOpenBluetooth = onOpenBluetooth,
                            onTroubleshooting = onTroubleshooting,
                        )
                    }
                }
                else -> PairingGuide(onOpenBluetooth, onTroubleshooting, onTryDemo)
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
private fun DashboardLoadingSkeleton() {
    val palette = LocalAndpopPalette.current
    val pulse = rememberInfiniteTransition(label = "Dashboard skeleton")
    val alpha by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "Skeleton alpha",
    )
    Column(Modifier.fillMaxWidth().alpha(alpha)) {
        Box(Modifier.fillMaxWidth().height(252.dp).clip(RoundedCornerShape(28.dp)).background(palette.s2))
        Spacer(Modifier.height(24.dp))
        Box(Modifier.width(120.dp).height(18.dp).padding(start = 8.dp).clip(RoundedCornerShape(9.dp)).background(palette.s2))
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(118.dp).clip(RoundedCornerShape(24.dp)).background(palette.s2))
        Spacer(Modifier.height(24.dp))
        Box(Modifier.width(90.dp).height(18.dp).padding(start = 8.dp).clip(RoundedCornerShape(9.dp)).background(palette.s2))
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(24.dp)).background(palette.s2))
    }
}

@Composable
private fun DashboardTopBar(
    onAccount: () -> Unit,
    onAppSettings: () -> Unit,
    accountInitials: String? = null,
) {
    val palette = LocalAndpopPalette.current
    val accountLabel = stringResource(R.string.andpop_account)
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.andpop_mark), null, Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)))
        Text(
            stringResource(R.string.app_name),
            Modifier.weight(1f).padding(start = 4.dp),
            color = palette.on,
            style = AndpopType.titleSmall,
        )
        IconButton(
            onClick = onAccount,
            modifier = Modifier.semantics { contentDescription = accountLabel },
        ) {
            if (!accountInitials.isNullOrBlank()) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(palette.pc), contentAlignment = Alignment.Center) {
                    Text(accountInitials, color = palette.onPc, style = AndpopType.label)
                }
            } else {
                Box(Modifier.size(40.dp).clip(CircleShape).background(palette.s2), contentAlignment = Alignment.Center) {
                    Icon(AndpopIcons.Person, accountLabel, Modifier.size(22.dp), tint = palette.on)
                }
            }
        }
        IconButton(onClick = onAppSettings) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(palette.s2), contentAlignment = Alignment.Center) {
                Icon(AndpopIcons.Tune, stringResource(R.string.settings), Modifier.size(22.dp), tint = palette.on)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroCard(
    title: String,
    model: AirPodsBase?,
    status: Triple<String, PillTone, Boolean>,
    content: @Composable () -> Unit,
) {
    val palette = LocalAndpopPalette.current
    val shape = RoundedCornerShape(28.dp)
    val top = lerp(palette.s1, palette.pc, 0.48f)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(0f to top, 0.7f to palette.s1))
            .border(1.dp, palette.on.copy(alpha = 0.07f), shape)
            .padding(start = 16.dp, end = 12.dp, top = 18.dp, bottom = 4.dp),
    ) {
        Column(Modifier.padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, Modifier.semantics { heading() }, color = palette.on, style = AndpopType.title)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                val modelLabel = when (model) {
                    is AirPodsPro2USBC -> stringResource(R.string.andpop_pro2_usb)
                    is AirPodsPro2Lightning -> stringResource(R.string.andpop_pro2_lightning)
                    else -> model?.name
                }
                if (!modelLabel.isNullOrBlank()) {
                    Text(modelLabel, color = palette.on2, style = AndpopType.bodySmall)
                }
                StatusPill(status.first, status.second, pulsing = status.third)
            }
        }
        content()
    }
}

@Composable
private fun DisconnectedActions(
    reconnecting: Boolean,
    showLastReadingNote: Boolean,
    onReconnect: (() -> Unit)?,
    onOpenBluetooth: () -> Unit,
    onTroubleshooting: () -> Unit,
) {
    val palette = LocalAndpopPalette.current
    Column(Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (onReconnect != null) {
            Button(
                onClick = onReconnect,
                enabled = !reconnecting,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.pri, contentColor = palette.onPri),
            ) {
                if (reconnecting) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = palette.on2, strokeWidth = 2.dp)
                    Spacer(Modifier.size(10.dp))
                    Text(stringResource(R.string.andpop_connecting), style = AndpopType.button)
                } else {
                    Icon(AndpopIcons.Bluetooth, null, Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.andpop_reconnect), style = AndpopType.button)
                }
            }
        }
        FilledTonalButton(
            onClick = onOpenBluetooth,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = palette.pc, contentColor = palette.onPc),
        ) {
            Text(stringResource(R.string.andpop_bluetooth_settings), style = AndpopType.button)
        }
        TextButton(onClick = onTroubleshooting, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.troubleshooting), color = palette.pri, style = AndpopType.button)
        }
        if (showLastReadingNote) {
            Text(
                stringResource(R.string.andpop_last_reading_note),
                Modifier.fillMaxWidth().padding(top = 4.dp),
                color = palette.on3,
                style = AndpopType.caption,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PairingGuide(
    onOpenBluetooth: () -> Unit,
    onTroubleshooting: () -> Unit,
    onTryDemo: (() -> Unit)? = null,
) {
    val palette = LocalAndpopPalette.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.padding(top = 20.dp, bottom = 14.dp).size(132.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(palette.pc, palette.pc.copy(alpha = 0.25f)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AndpopIcons.Buds, null, Modifier.size(56.dp), tint = palette.onPc)
        }
        Text(
            stringResource(R.string.andpop_pair_title), Modifier.semantics { heading() },
            color = palette.on, style = AndpopType.pageTitle, textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.andpop_pair_intro), Modifier.padding(top = 8.dp, bottom = 20.dp),
            color = palette.on2, style = AndpopType.bodySmall, textAlign = TextAlign.Center,
        )
        val steps = listOf(R.string.andpop_pair_step_1, R.string.andpop_pair_step_2, R.string.andpop_pair_step_3)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            steps.forEachIndexed { index, step ->
                Row(
                    Modifier.fillMaxWidth().clip(me.kavishdevar.librepods.presentation.components.groupShape(index, steps.size))
                        .background(palette.s1).padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(26.dp).clip(CircleShape).background(palette.pc), contentAlignment = Alignment.Center) {
                        Text("${index + 1}", color = palette.onPc, style = AndpopType.label)
                    }
                    Text(stringResource(step), Modifier.weight(1f), color = palette.on, style = AndpopType.bodySmall)
                }
            }
        }
        Button(
            onClick = onOpenBluetooth,
            modifier = Modifier.padding(top = 20.dp).fillMaxWidth().heightIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = palette.pri, contentColor = palette.onPri),
        ) {
            Icon(AndpopIcons.Bluetooth, null, Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.andpop_open_bluetooth_settings), style = AndpopType.button)
        }
        if (onTryDemo != null) {
            TextButton(onClick = onTryDemo, modifier = Modifier.padding(top = 4.dp).fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.andpop_try_demo), color = palette.pri, style = AndpopType.button)
            }
        }
        TextButton(onClick = onTroubleshooting, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.troubleshooting), color = palette.pri, style = AndpopType.button)
        }
    }
}

@Composable
internal fun DashboardHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier.semantics { heading() },
        color = LocalAndpopPalette.current.on2, style = AndpopType.section,
    )
}

@Preview(name = "Dashboard · Dark", widthDp = 384, heightDp = 832, showBackground = true, uiMode = 0x20)
@Preview(name = "Dashboard · Light", widthDp = 384, heightDp = 832, showBackground = true, uiMode = 0x10)
@Composable
private fun AndpopDashboardPreview() = PreviewDashboard("A3047", "AirPods Pro 2")

@Preview(name = "AirPods Max · Dark", widthDp = 384, heightDp = 832, showBackground = true, uiMode = 0x20)
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
            caseReading = CaseBatteryReading(64, charging = false, estimated = false, observedAt = 0),
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
