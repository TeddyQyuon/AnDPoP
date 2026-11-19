// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.presentation.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.AirPodsBase
import me.kavishdevar.librepods.data.AirPodsPro2Lightning
import me.kavishdevar.librepods.data.AirPodsPro2USBC
import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryComponent
import me.kavishdevar.librepods.data.CaseBatteryReading
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.presentation.theme.devicePanel
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsUiState

internal data class DashboardBattery(
    val art: DeviceArtKind,
    @param:StringRes val label: Int,
    val level: Int?,
    val charging: Boolean,
    val estimated: Boolean = false,
    val lastSeen: Boolean = false,
) {
    val low = level != null && level <= 20 && !charging && !lastSeen
    val detail: Int?
        @StringRes get() = when {
            level == null -> R.string.andpop_no_reading
            lastSeen -> R.string.andpop_last_seen
            charging -> R.string.andpop_charging
            estimated -> R.string.andpop_estimated
            low -> R.string.andpop_low_battery
            else -> null
        }
}

internal fun dashboardBattery(
    state: AirPodsUiState,
    connected: Boolean,
    headphones: Boolean,
    caseReading: CaseBatteryReading?,
    caseCurrent: Boolean,
): List<DashboardBattery> = buildList {
    if (connected) {
        if (headphones) {
            val reading = state.battery.firstOrNull { it.component == BatteryComponent.HEADPHONES && it.hasValidReading }
                ?: state.battery.firstOrNull { it.component != BatteryComponent.CASE && it.hasValidReading }
            add(reading.tile(DeviceArtKind.Headphones, R.string.andpop_headphones))
        } else {
            val left = state.battery.firstOrNull { it.component == BatteryComponent.LEFT }
            val right = state.battery.firstOrNull { it.component == BatteryComponent.RIGHT }
            add(left.tile(DeviceArtKind.Left, R.string.andpop_left))
            add(right.tile(DeviceArtKind.Right, R.string.andpop_right))
        }
    }
    if (!headphones && (connected || state.caseBattery.deviceAddress != null || caseReading != null)) {
        add(
            DashboardBattery(
                art = DeviceArtKind.Case,
                label = R.string.andpop_case,
                level = caseReading?.level,
                charging = caseReading?.charging == true && caseCurrent,
                estimated = caseReading?.estimated == true,
                lastSeen = caseReading != null && !caseCurrent,
            ),
        )
    }
}

private fun Battery?.tile(art: DeviceArtKind, @StringRes label: Int): DashboardBattery {
    val reading = this?.takeIf { it.hasValidReading }
    return DashboardBattery(art, label, reading?.level, reading?.isCharging == true)
}

@Composable
internal fun BatterySection(
    items: List<DashboardBattery>,
    model: AirPodsBase?,
    caseNeedsReading: Boolean,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onQuickView: () -> Unit,
) {
    val palette = LocalAndpopPalette.current
    val colors = palette.scheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            Modifier.fillMaxWidth().devicePanel(palette),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 5.dp, top = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DashboardHeading(stringResource(R.string.andpop_battery), Modifier.weight(1f))
                IconButton(onClick = onRefresh, enabled = !refreshing, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Refresh, stringResource(R.string.andpop_refresh))
                }
            }

            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
                val sideBySide = maxWidth >= 288.dp && LocalDensity.current.fontScale <= 1.25f && items.size > 1
                val headphones = items.singleOrNull()?.takeIf { it.art == DeviceArtKind.Headphones }
                if (headphones != null) {
                    BatteryCell(headphones, model, Modifier.fillMaxWidth().padding(vertical = 10.dp))
                } else if (sideBySide) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items.forEachIndexed { index, item ->
                            if (index > 0) {
                                VerticalDivider(
                                    Modifier.size(width = 1.dp, height = 104.dp),
                                    color = colors.outlineVariant,
                                )
                            }
                            BatteryCell(item, model, Modifier.weight(1f))
                        }
                    }
                } else {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
                        items.forEachIndexed { index, item ->
                            if (index > 0) HorizontalDivider(color = colors.outlineVariant)
                            BatteryRow(item, model)
                        }
                    }
                }
            }

            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = colors.outlineVariant)
            TextButton(
                onClick = onQuickView,
                modifier = Modifier.align(Alignment.End).padding(end = 6.dp),
            ) {
                Text(stringResource(R.string.andpop_battery_details))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(20.dp))
            }
        }

        if (refreshing || caseNeedsReading) {
            Text(
                stringResource(if (refreshing) R.string.andpop_checking_battery else R.string.andpop_case_hint),
                Modifier.padding(horizontal = 4.dp).semantics { liveRegion = LiveRegionMode.Polite },
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun BatteryCell(item: DashboardBattery, model: AirPodsBase?, modifier: Modifier) {
    val palette = LocalAndpopPalette.current
    Column(
        modifier.padding(horizontal = 2.dp).heightIn(min = 125.dp).semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        val artSize = if (item.art == DeviceArtKind.Headphones) 96.dp else 63.dp
        BatteryArtwork(item, model, Modifier.size(artSize))
        BatteryValue(item)
        Text(
            stringResource(item.label),
            color = palette.scheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
        )
        BatteryDetail(item)
    }
}

@Composable
private fun BatteryArtwork(item: DashboardBattery, model: AirPodsBase?, modifier: Modifier) {
    val art = batteryArtwork(model, item.art)
    if (art != 0) {
        val photoModifier = if (item.art == DeviceArtKind.Headphones) {
            modifier.clip(RoundedCornerShape(12.dp))
        } else {
            modifier
        }
        Image(painterResource(art), null, photoModifier)
    } else {
        DeviceArt(item.art, modifier, LocalAndpopPalette.current.scheme.onSurfaceVariant)
    }
}

@Composable
private fun BatteryRow(item: DashboardBattery, model: AirPodsBase?) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BatteryArtwork(item, model, Modifier.size(39.dp))
        Text(
            stringResource(if (item.art == DeviceArtKind.Case) R.string.andpop_case_full else item.label),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Column(horizontalAlignment = Alignment.End) {
            BatteryValue(item, showGlyph = true)
            BatteryDetail(item)
        }
    }
}

@Composable
private fun BatteryValue(item: DashboardBattery, showGlyph: Boolean = false) {
    val palette = LocalAndpopPalette.current
    val colors = palette.scheme
    val ink = when {
        item.lastSeen || item.level == null -> colors.onSurfaceVariant
        item.low -> colors.error
        else -> colors.onSurface
    }
    val value = when {
        item.level == null -> stringResource(R.string.andpop_battery_empty)
        item.estimated -> stringResource(R.string.andpop_estimated_value, item.level)
        else -> stringResource(R.string.andpop_battery_value, item.level)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(value, color = ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (showGlyph && item.level != null) {
            BatteryGlyph(item.level, if (item.charging) palette.connected else if (item.low) ink else colors.primary)
        }
    }
}

@Composable
private fun BatteryDetail(item: DashboardBattery) {
    val palette = LocalAndpopPalette.current
    val colors = palette.scheme
    item.detail?.let {
        Text(
            stringResource(it),
            color = if (item.low) colors.error else if (item.charging) palette.connected else colors.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun BatteryGlyph(level: Int, ink: Color) {
    Canvas(Modifier.size(width = 20.dp, height = 10.dp)) {
        if (size.width < 8.dp.toPx()) return@Canvas
        val stroke = 1.dp.toPx()
        val body = size.width - 3.dp.toPx()
        val radius = CornerRadius(2.dp.toPx())
        drawRoundRect(
            ink.copy(alpha = 0.78f), Offset(stroke / 2, stroke / 2),
            Size(body - stroke, size.height - stroke), radius, style = Stroke(stroke),
        )
        val inset = 2.dp.toPx()
        val chargeWidth = (body - inset * 2) * level.coerceIn(0, 100) / 100f
        if (chargeWidth > 0f) {
            drawRoundRect(ink, Offset(inset, inset), Size(chargeWidth, size.height - inset * 2), CornerRadius(stroke))
        }
        drawRoundRect(
            ink.copy(alpha = 0.78f), Offset(body + stroke, size.height * 0.28f),
            Size(stroke, size.height * 0.44f), CornerRadius(stroke),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BatteryQuickView(
    deviceName: String,
    model: AirPodsBase?,
    items: List<DashboardBattery>,
    onDismiss: () -> Unit,
) {
    val palette = LocalAndpopPalette.current
    val colors = palette.scheme
    val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        shape = shape,
        containerColor = Color.Transparent,
        contentColor = colors.onSurface,
        scrimColor = colors.background.copy(alpha = 0.42f),
        dragHandle = null,
    ) {
        Column(
            Modifier.fillMaxWidth().devicePanel(palette, shape, colors.surface.copy(alpha = 0.94f)).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = colors.onSurfaceVariant.copy(alpha = 0.60f),
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        stringResource(R.string.andpop_battery), Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    )
                    Text(deviceName, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp).devicePanel(palette, RoundedCornerShape(50), colors.surfaceContainerHigh),
                ) {
                    Icon(Icons.Default.Close, stringResource(R.string.andpop_close_battery), tint = colors.primary)
                }
            }
            Column(Modifier.fillMaxWidth().devicePanel(palette, color = colors.surfaceContainerLow).padding(horizontal = 14.dp, vertical = 2.dp)) {
                items.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(color = colors.outlineVariant)
                    BatteryRow(item, model)
                }
            }
        }
    }
}

@DrawableRes
private fun batteryArtwork(model: AirPodsBase?, kind: DeviceArtKind): Int {
    val usesPhoto = model is AirPodsPro2USBC || model is AirPodsPro2Lightning
    return when (kind) {
        DeviceArtKind.Left -> if (usesPhoto) R.drawable.amber_pro_2_left else model?.leftBudsRes ?: 0
        DeviceArtKind.Right -> if (usesPhoto) R.drawable.amber_pro_2_right else model?.rightBudsRes ?: 0
        DeviceArtKind.Case -> if (usesPhoto) R.drawable.amber_pro_2_case else model?.caseRes ?: 0
        DeviceArtKind.Headphones -> R.drawable.andpop_max_photo
        else -> 0
    }
}
