// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.presentation.screens

import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager.Companion.ControlCommandIdentifiers
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.data.NoiseControlMode
import me.kavishdevar.librepods.presentation.components.AndpopCard
import me.kavishdevar.librepods.presentation.components.AndpopSectionHeader
import me.kavishdevar.librepods.presentation.theme.AndpopType
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsUiState

private enum class ListeningOption(
    val mode: NoiseControlMode,
    @param:StringRes val label: Int,
    @param:StringRes val description: Int,
) {
    CancelNoise(NoiseControlMode.NOISE_CANCELLATION, R.string.andpop_cancel_noise, R.string.andpop_anc_description),
    Transparency(NoiseControlMode.TRANSPARENCY, R.string.transparency, R.string.andpop_transparency_description),
    Adaptive(NoiseControlMode.ADAPTIVE, R.string.adaptive, R.string.andpop_adaptive_description),
    Off(NoiseControlMode.OFF, R.string.off, R.string.andpop_off_description),
}

/** Space a tile adds around its label (horizontal padding on both sides plus a small margin). */
private val TilePadding = 20.dp
private val TrackPadding = 4.dp
private val TileGap = 4.dp

@Composable
internal fun DashboardListeningModes(state: AirPodsUiState, onSelectMode: (NoiseControlMode) -> Unit) {
    val palette = LocalAndpopPalette.current
    val current = state.controlStates[ControlCommandIdentifiers.LISTENING_MODE]?.firstOrNull()?.toInt()
    val options = buildList {
        add(ListeningOption.CancelNoise)
        add(ListeningOption.Transparency)
        if (Capability.ADAPTIVE_AUDIO in state.capabilities) add(ListeningOption.Adaptive)
        if (state.offListeningMode) add(ListeningOption.Off)
    }
    val selected = options.firstOrNull { it.mode.ordinal + 1 == current }
    Column {
        AndpopSectionHeader(stringResource(R.string.andpop_listening_mode))
        AndpopCard {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(8.dp).selectableGroup()) {
                val measurer = rememberTextMeasurer()
                val density = LocalDensity.current
                val style = AndpopType.label
                // Measure each label (word by word) so translated text and large font sizes never split a word.
                val widths = options.associateWith { option ->
                    val words = stringResource(option.label).split(' ')
                    val pixels = words.maxOf { measurer.measure(it, style).size.width }
                    with(density) { pixels.toDp() + TilePadding }.coerceAtLeast(56.dp)
                }
                val inner = maxWidth - TrackPadding * 2
                val total = widths.values.reduce { a, b -> a + b } + TileGap * (options.size - 1)
                val columns = when {
                    maxWidth >= 298.dp && total <= inner -> options.size
                    widths.values.max() * 2 + TileGap <= inner -> 2
                    else -> 1
                }
                ModeTrack(options, selected, columns, widths, onSelectMode)
            }
            Crossfade(targetState = selected, label = "Listening mode description") { option ->
                Text(
                    if (option != null) stringResource(option.description) else "",
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 14.dp),
                    color = palette.on2,
                    style = AndpopType.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ModeTrack(
    options: List<ListeningOption>,
    selected: ListeningOption?,
    columns: Int,
    widths: Map<ListeningOption, androidx.compose.ui.unit.Dp>,
    onSelectMode: (NoiseControlMode) -> Unit,
) {
    val palette = LocalAndpopPalette.current
    val density = LocalDensity.current
    val oneRow = columns == options.size
    // Tile bounds inside the row (left, width) in pixels, used to slide the selection indicator.
    val bounds = remember { mutableStateMapOf<ListeningOption, Pair<Int, Int>>() }
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(palette.s2).padding(TrackPadding),
    ) {
        val target = selected?.let { bounds[it] }
        if (oneRow && target != null) {
            val motion = spring<Int>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
            val left by animateIntAsState(target.first, motion, label = "Indicator offset")
            val width by animateIntAsState(target.second, motion, label = "Indicator width")
            Box(Modifier.matchParentSize()) {
                Box(
                    Modifier.offset { IntOffset(left, 0) }.width(with(density) { width.toDp() }).fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp)).background(palette.pc),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(TileGap)) {
            options.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TileGap)) {
                    row.forEach { option ->
                        val share = if (oneRow) widths.getValue(option).value else 1f
                        ListeningModeButton(
                            option = option,
                            selected = option == selected,
                            tinted = !oneRow,
                            horizontal = columns == 1,
                            onSelect = { onSelectMode(option.mode) },
                            modifier = Modifier.weight(share).onGloballyPositioned { coordinates ->
                                bounds[option] = coordinates.positionInParent().x.toInt() to coordinates.size.width
                            },
                        )
                    }
                    repeat(columns - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ListeningModeButton(
    option: ListeningOption,
    selected: Boolean,
    tinted: Boolean,
    horizontal: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier,
) {
    val palette = LocalAndpopPalette.current
    val ink by animateColorAsState(if (selected) palette.onPc else palette.on2, label = "Mode ink")
    val fill by animateColorAsState(if (selected && tinted) palette.pc else Color.Transparent, label = "Mode fill")
    Box(
        modifier.clip(RoundedCornerShape(18.dp)).background(fill)
            .selectable(selected, role = Role.RadioButton, onClick = onSelect)
            .heightIn(min = if (horizontal) 52.dp else 76.dp)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (horizontal) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                ListeningModeGlyph(option.mode, ink)
                Text(stringResource(option.label), color = ink, style = AndpopType.label)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ListeningModeGlyph(option.mode, ink)
                Text(
                    stringResource(option.label), color = ink,
                    modifier = Modifier.fillMaxWidth(),
                    style = AndpopType.label,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (selected) {
            val check = palette.selectedCheck
            Canvas(Modifier.align(Alignment.TopEnd).size(12.dp)) {
                val elbow = Offset(size.width * 0.38f, size.height * 0.77f)
                drawLine(check, Offset(size.width * 0.08f, size.height * 0.48f), elbow, 2.dp.toPx(), StrokeCap.Round)
                drawLine(check, elbow, Offset(size.width * 0.92f, size.height * 0.16f), 2.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun ListeningModeGlyph(mode: NoiseControlMode, ink: Color) {
    Canvas(Modifier.size(22.dp)) {
        val unit = size.minDimension / 24f
        val stroke = unit * 1.7f
        fun point(x: Float, y: Float) = Offset(x * unit, y * unit)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(ink, point(x1, y1), point(x2, y2), stroke, StrokeCap.Round)

        when (mode) {
            NoiseControlMode.NOISE_CANCELLATION -> {
                val shield = Path().apply {
                    moveTo(12 * unit, 2.8f * unit)
                    lineTo(20 * unit, 6.5f * unit)
                    lineTo(20 * unit, 11 * unit)
                    cubicTo(20 * unit, 16.8f * unit, 16 * unit, 20 * unit, 12 * unit, 21.2f * unit)
                    cubicTo(8 * unit, 20 * unit, 4 * unit, 16.8f * unit, 4 * unit, 11 * unit)
                    lineTo(4 * unit, 6.5f * unit)
                    close()
                }
                drawPath(shield, ink, style = Stroke(stroke))
                line(8.5f, 12f, 15.5f, 12f)
            }
            NoiseControlMode.TRANSPARENCY -> {
                drawCircle(ink, 2.1f * unit)
                val waves = Path().apply {
                    moveTo(6.8f * unit, 6.7f * unit)
                    quadraticTo(1.4f * unit, 12 * unit, 6.8f * unit, 17.3f * unit)
                    moveTo(17.2f * unit, 6.7f * unit)
                    quadraticTo(22.6f * unit, 12 * unit, 17.2f * unit, 17.3f * unit)
                }
                drawPath(waves, ink, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            NoiseControlMode.ADAPTIVE -> {
                listOf(5f, 10f, 17f, 10f, 5f).forEachIndexed { index, height ->
                    val x = 3f + index * 4.5f
                    line(x, 12f - height / 2, x, 12f + height / 2)
                }
            }
            NoiseControlMode.OFF -> {
                drawCircle(ink, 8.4f * unit, style = Stroke(stroke))
                line(6f, 18f, 18f, 6f)
            }
        }
    }
}
