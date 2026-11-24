// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.presentation.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.AirPodsBase
import me.kavishdevar.librepods.data.AirPodsPro2Lightning
import me.kavishdevar.librepods.data.AirPodsPro2USBC
import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryComponent
import me.kavishdevar.librepods.data.CaseBatteryReading
import me.kavishdevar.librepods.presentation.components.AndpopGroup
import me.kavishdevar.librepods.presentation.components.InfoBanner
import me.kavishdevar.librepods.presentation.theme.AndpopIcons
import me.kavishdevar.librepods.presentation.theme.AndpopType
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
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

/**
 * Battery cells, the refresh control and Battery details. On the dashboard this sits inside the
 * device hero card.
 */
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
    Column(Modifier.fillMaxWidth()) {
        if (items.all { it.level == null }) {
            Text(
                stringResource(if (refreshing) R.string.andpop_checking_battery else R.string.andpop_battery_unavailable),
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 22.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                color = palette.on,
                style = AndpopType.body,
                textAlign = TextAlign.Center,
            )
        } else BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            val sideBySide = maxWidth >= 288.dp && LocalDensity.current.fontScale <= 1.25f && items.size > 1
            val headphones = items.singleOrNull()?.takeIf { it.art == DeviceArtKind.Headphones }
            if (headphones != null) {
                HeadphonesCell(headphones)
            } else if (sideBySide) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    items.forEachIndexed { index, item ->
                        if (index > 0) {
                            Box(Modifier.width(1.dp).height(72.dp).background(palette.on.copy(alpha = 0.12f)))
                        }
                        BatteryCell(item, model, Modifier.weight(1f))
                    }
                }
            } else {
                Column(Modifier.fillMaxWidth()) {
                    items.forEach { BatteryRow(it, model, Modifier.padding(vertical = 4.dp)) }
                }
            }
        }

        Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(1.dp).background(palette.on.copy(alpha = 0.08f)))
        Row(
            Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Spin only while a refresh is running so the dashboard stays idle otherwise.
            val angle = if (refreshing) {
                val spin = rememberInfiniteTransition(label = "Battery refresh")
                spin.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "Refresh angle").value
            } else {
                0f
            }
            IconButton(onClick = onRefresh, enabled = !refreshing) {
                Icon(
                    AndpopIcons.Refresh, stringResource(R.string.andpop_refresh),
                    Modifier.size(20.dp).rotate(angle),
                    tint = palette.on2,
                )
            }
            Text(
                if (refreshing) stringResource(R.string.andpop_checking_battery) else "",
                Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
                color = palette.on3,
                style = AndpopType.caption,
                maxLines = 1,
            )
            TextButton(onClick = onQuickView) {
                Text(stringResource(R.string.andpop_battery_details), color = palette.pri, style = AndpopType.button)
                Icon(AndpopIcons.ChevronRight, null, Modifier.size(18.dp), tint = palette.pri)
            }
        }

        if (caseNeedsReading && !refreshing) {
            InfoBanner(stringResource(R.string.andpop_case_hint), Modifier.padding(top = 4.dp, bottom = 6.dp))
        }
    }
}

@Composable
private fun BatteryCell(item: DashboardBattery, model: AirPodsBase?, modifier: Modifier) {
    val palette = LocalAndpopPalette.current
    Column(
        modifier.padding(horizontal = 2.dp).heightIn(min = 132.dp).semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BatteryArtwork(item, model, Modifier.height(76.dp).fillMaxWidth().padding(bottom = 6.dp))
        BatteryValue(item)
        Text(stringResource(item.label), color = palette.on2, style = AndpopType.caption, textAlign = TextAlign.Center)
        Box(Modifier.heightIn(min = 24.dp).padding(top = 4.dp), contentAlignment = Alignment.Center) { BatteryDetail(item) }
    }
}

@Composable
private fun HeadphonesCell(item: DashboardBattery) {
    val palette = LocalAndpopPalette.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MaxPhoto(Modifier.width(124.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BatteryValue(item, large = true)
            Text(stringResource(item.label), color = palette.on2, style = AndpopType.caption)
            BatteryDetail(item)
        }
    }
}

/** The AirPods Max photo has a white background, so it sits in a light rounded well. */
@Composable
private fun MaxPhoto(modifier: Modifier) {
    Box(
        modifier.aspectRatio(330f / 315f).clip(RoundedCornerShape(22.dp)).background(Color(0xFFF3F5F6)),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.andpop_max_photo), null, Modifier.fillMaxWidth(), contentScale = ContentScale.Crop)
    }
}

@Composable
private fun BatteryArtwork(item: DashboardBattery, model: AirPodsBase?, modifier: Modifier) {
    val art = batteryArtwork(model, item.art)
    val dim = item.lastSeen || item.level == null
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        if (item.art == DeviceArtKind.Headphones) {
            MaxPhoto(Modifier.fillMaxHeight())
        } else if (art != 0) {
            Image(
                painterResource(art), null,
                Modifier.fillMaxHeight(),
                alpha = if (dim) 0.55f else 1f,
                contentScale = ContentScale.Fit,
            )
        } else {
            DeviceArt(item.art, Modifier.size(56.dp), LocalAndpopPalette.current.on2)
        }
    }
}

@Composable
private fun BatteryRow(item: DashboardBattery, model: AirPodsBase?, modifier: Modifier = Modifier) {
    val palette = LocalAndpopPalette.current
    Row(
        modifier.fillMaxWidth().heightIn(min = 60.dp).semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BatteryArtwork(item, model, Modifier.size(44.dp))
        Text(
            stringResource(if (item.art == DeviceArtKind.Case) R.string.andpop_case_full else item.label),
            Modifier.weight(1f),
            color = palette.on,
            style = AndpopType.body,
        )
        Column(horizontalAlignment = Alignment.End) {
            BatteryValue(item)
            BatteryDetail(item)
        }
    }
}

@Composable
private fun BatteryValue(item: DashboardBattery, large: Boolean = false) {
    val palette = LocalAndpopPalette.current
    val ink = when {
        item.lastSeen || item.level == null -> palette.on3
        item.low -> palette.warning
        else -> palette.on
    }
    val value = when {
        item.level == null -> stringResource(R.string.andpop_battery_empty)
        item.estimated -> stringResource(R.string.andpop_estimated_value, item.level)
        else -> stringResource(R.string.andpop_battery_value, item.level)
    }
    Text(value, color = ink, style = if (large) AndpopType.battery.copy(fontSize = AndpopType.battery.fontSize * 1.6f, lineHeight = AndpopType.battery.lineHeight * 1.6f) else AndpopType.battery)
}

@Composable
private fun BatteryDetail(item: DashboardBattery) {
    val palette = LocalAndpopPalette.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val level = item.level
        if (level != null && !item.lastSeen) {
            BatteryGauge(level, item.activelyCharging, item.low)
        }
        if (!item.activelyCharging) {
            item.detail?.let {
                Text(
                    stringResource(it),
                    color = if (item.low) palette.warning else palette.on3,
                    style = AndpopType.tiny,
                )
            }
        }
    }
}

/**
 * Horizontal battery with a proportional fill. While charging the fill turns green and a dark
 * bolt sits on top. Decided per component: level != null && charging && !lastSeen.
 */
@Composable
private fun BatteryGauge(level: Int, charging: Boolean, low: Boolean = false, width: Dp = 36.dp) {
    val palette = LocalAndpopPalette.current
    val description = if (charging) stringResource(R.string.andpop_charging) else null
    val shell = palette.on2.copy(alpha = 0.8f)
    val fill = when {
        charging -> palette.charge
        low -> palette.warning
        else -> palette.on
    }
    Canvas(Modifier.size(width, width / 2).semantics {
        progressBarRangeInfo = ProgressBarRangeInfo(level.toFloat(), 0f..100f)
        if (description != null) contentDescription = description
    }) {
        withTransform({ scale(size.width / 36f, size.height / 18f, Offset.Zero) }) {
            drawRoundRect(shell, Offset(1f, 1.5f), Size(29.5f, 15f), CornerRadius(4.6f), style = Stroke(1.5f))
            drawRoundRect(shell, Offset(32f, 6f), Size(2.6f, 6f), CornerRadius(1.2f))
            drawRoundRect(fill, Offset(3.5f, 4f), Size((level.coerceIn(0, 100) / 100f * 25f).coerceAtLeast(2f), 10f), CornerRadius(2.6f))
            if (!charging) return@withTransform
            val bolt = Path().apply {
                moveTo(19.6f, 1.6f)
                lineTo(12.4f, 10.4f)
                lineTo(17.0f, 10.4f)
                lineTo(15.1f, 17.2f)
                lineTo(22.7f, 7.8f)
                lineTo(18.0f, 7.8f)
                close()
            }
            drawPath(bolt, palette.bolt)
            drawPath(bolt, palette.boltEdge, style = Stroke(1f, join = StrokeJoin.Round))
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
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        shape = shape,
        containerColor = palette.s1,
        contentColor = palette.on,
        scrimColor = Color.Black.copy(alpha = if (palette.dark) 0.55f else 0.36f),
        dragHandle = null,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp).size(32.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp)).background(palette.on3.copy(alpha = 0.6f)),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.andpop_battery), Modifier.semantics { heading() }, color = palette.on, style = AndpopType.title)
                    Text(deviceName, color = palette.on2, style = AndpopType.bodySmall)
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).background(palette.s2),
                ) {
                    Icon(AndpopIcons.Close, stringResource(R.string.andpop_close_battery), Modifier.size(22.dp), tint = palette.on)
                }
            }
            AndpopGroup {
                items.forEach { item -> row { shape -> SheetBatteryRow(item, model, shape) } }
            }
            InfoBanner(stringResource(R.string.andpop_battery_note))
        }
    }
}

@Composable
private fun SheetBatteryRow(item: DashboardBattery, model: AirPodsBase?, shape: androidx.compose.ui.graphics.Shape) {
    val palette = LocalAndpopPalette.current
    val status = when {
        item.level == null -> stringResource(R.string.andpop_no_reading)
        item.activelyCharging -> stringResource(R.string.andpop_charging)
        item.lastSeen -> stringResource(R.string.andpop_last_seen)
        item.estimated -> stringResource(R.string.andpop_estimated_detail)
        item.low -> stringResource(R.string.andpop_low_battery)
        else -> stringResource(R.string.andpop_not_charging)
    }
    val statusInk = when {
        item.activelyCharging -> palette.charge
        item.low -> palette.warning
        else -> palette.on2
    }
    val barColor = when {
        item.activelyCharging -> palette.charge
        item.low -> palette.warning
        item.lastSeen -> palette.on3
        else -> palette.pri
    }
    val fraction by animateFloatAsState((item.level ?: 0).coerceIn(0, 100) / 100f, label = "Battery level")
    Row(
        Modifier.fillMaxWidth().heightIn(min = 76.dp).clip(shape).background(palette.s2)
            .padding(horizontal = 16.dp, vertical = 12.dp).semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BatteryArtwork(item, model, Modifier.size(48.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(if (item.art == DeviceArtKind.Case) R.string.andpop_case_full else item.label),
                color = palette.on, style = AndpopType.body,
            )
            Text(status, color = statusInk, style = AndpopType.supporting)
            if (item.level != null) {
                Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(palette.s4)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).clip(RoundedCornerShape(3.dp)).background(barColor))
                }
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BatteryValue(item)
            if (item.level != null && !item.lastSeen) BatteryGauge(item.level, item.activelyCharging, item.low, 32.dp)
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
