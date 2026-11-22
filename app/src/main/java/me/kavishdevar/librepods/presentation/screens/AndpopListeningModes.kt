// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.presentation.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager.Companion.ControlCommandIdentifiers
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.data.NoiseControlMode
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.presentation.theme.devicePanel
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

@Composable
internal fun DashboardListeningModes(state: AirPodsUiState, onSelectMode: (NoiseControlMode) -> Unit) {
    val palette = LocalAndpopPalette.current
    val colors = palette.scheme
    val current = state.controlStates[ControlCommandIdentifiers.LISTENING_MODE]?.firstOrNull()?.toInt()
    val options = buildList {
        add(ListeningOption.CancelNoise)
        add(ListeningOption.Transparency)
        if (Capability.ADAPTIVE_AUDIO in state.capabilities) add(ListeningOption.Adaptive)
        if (state.offListeningMode) add(ListeningOption.Off)
    }
    val selected = options.firstOrNull { it.mode.ordinal + 1 == current }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DashboardHeading(stringResource(R.string.listening_mode), Modifier.padding(horizontal = 4.dp))
        Column(
            Modifier.fillMaxWidth().devicePanel(palette).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth().selectableGroup()) {
                val fontScale = LocalDensity.current.fontScale
                val columns = when {
                    maxWidth >= 78.dp * options.size && fontScale <= 1.22f -> options.size
                    maxWidth >= 225.dp && fontScale <= 1.5f -> 2
                    else -> 1
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.chunked(columns).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { option ->
                                ListeningModeButton(
                                    option = option,
                                    selected = option == selected,
                                    onSelect = { onSelectMode(option.mode) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(columns - row.size) { Box(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            selected?.let {
                Text(
                    stringResource(it.description),
                    Modifier.padding(start = 7.dp, end = 7.dp, bottom = 4.dp),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ListeningModeButton(
    option: ListeningOption,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier,
) {
    val palette = LocalAndpopPalette.current
    Box(
        modifier.devicePanel(palette, RoundedCornerShape(15.dp), palette.scheme.surfaceContainerHigh)
            .selectable(selected, role = Role.RadioButton, onClick = onSelect)
            .heightIn(min = 76.dp).padding(horizontal = 2.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ListeningModeGlyph(option.mode, palette.scheme.onSurface)
            Text(
                stringResource(option.label), color = palette.scheme.onSurface,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
        if (selected) {
            Canvas(Modifier.align(Alignment.TopEnd).padding(end = 2.dp).size(12.dp)) {
                val elbow = Offset(size.width * 0.38f, size.height * 0.77f)
                drawLine(palette.selectedCheck, Offset(size.width * 0.08f, size.height * 0.48f), elbow, 2.dp.toPx(), StrokeCap.Round)
                drawLine(palette.selectedCheck, elbow, Offset(size.width * 0.92f, size.height * 0.16f), 2.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun ListeningModeGlyph(mode: NoiseControlMode, ink: Color) {
    Canvas(Modifier.size(19.dp)) {
        val unit = size.minDimension / 24f
        val stroke = unit * 1.65f
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
