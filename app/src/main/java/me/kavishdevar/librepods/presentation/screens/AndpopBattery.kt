// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.presentation.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
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
    val activelyCharging get() = level != null && charging && !lastSeen
    val low = level != null && level <= 20 && !charging && !lastSeen
    val detail: Int?
        @StringRes get() = when {
            level == null -> R.string.andpop_no_reading
            lastSeen -> R.string.andpop_last_seen
            activelyCharging -> R.string.andpop_charging
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

            if (items.all { it.level == null }) {
                Text(
                    stringResource(if (refreshing) R.string.andpop_checking_battery else R.string.andpop_battery_unavailable),
                    Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface,
                )
            } else BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
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
                modifier = Modifier.align(Alignment.End).padding(end = 10.dp, bottom = 6.dp).devicePanel(palette, RoundedCornerShape(50), colors.surfaceContainerHigh),
            ) {
                Text(stringResource(R.string.andpop_battery_details))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(20.dp))
            }
        }

        if ((refreshing && items.any { it.level != null }) || (caseNeedsReading && !refreshing)) {
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
        val artSize = if (item.art == DeviceArtKind.Headphones) 96.dp else 72.dp
        BatteryArtwork(item, model, Modifier.size(artSize))
        BatteryValue(item)
        Text(
            stringResource(item.label),
            color = palette.scheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
        )
        Box(Modifier.heightIn(min = 24.dp), contentAlignment = Alignment.Center) { BatteryDetail(item) }
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
            BatteryValue(item)
            BatteryDetail(item)
        }
    }
}

@Composable
private fun BatteryValue(item: DashboardBattery) {
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
    Text(value, color = ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
}

@Composable
private fun BatteryDetail(item: DashboardBattery) {
    val colors = LocalAndpopPalette.current.scheme
    if (item.activelyCharging) {
        ChargingGlyph(requireNotNull(item.level), stringResource(R.string.andpop_charging))
    } else {
        item.detail?.let {
            Text(stringResource(it), color = if (item.low) colors.error else colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ChargingGlyph(level: Int, description: String) {
    val dark = LocalAndpopPalette.current.scheme.background.luminance() < 0.5f
    val shell = if (dark) Color(0xFFD5E3E4) else Color(0xFF304347)
    val well = if (dark) Color(0xFF12303A) else Color.White
    Canvas(Modifier.size(44.dp, 24.dp).semantics { contentDescription = description }) {
        withTransform({ scale(size.width / 48f, size.height / 25f, Offset.Zero) }) {
            val origin = Offset(1.5f, 4f)
            val body = Size(40.5f, 17f)
            val corner = CornerRadius(3.2f)
            drawRoundRect(well, origin, body, corner)
            drawRoundRect(shell, origin, body, corner, style = Stroke(2f))
            drawRoundRect(shell, Offset(44.3f, 9f), Size(2.8f, 7f), CornerRadius(1.1f))
            drawRoundRect(Color(0xFF69D447), Offset(4.5f, 7f),
                Size(level.coerceIn(0, 100) * 0.345f, 11f), CornerRadius(1.3f))
            val bolt = Path().apply {
                moveTo(26.8f, 1.3f)
                lineTo(17f, 14f)
                lineTo(23.1f, 14f)
                lineTo(20.7f, 23.6f)
                lineTo(31.2f, 10f)
                lineTo(24.9f, 10f)
                close()
            }
            drawPath(bolt, Color(0xFF101713))
            drawPath(bolt, Color(0xFFF1F9E9), style = Stroke(1.15f, join = StrokeJoin.Round))
        }
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
