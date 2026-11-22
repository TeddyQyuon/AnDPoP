// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.presentation.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager.Companion.ControlCommandIdentifiers
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.data.NoiseControlMode
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DashboardHeading(stringResource(R.string.listening_mode), Modifier.padding(horizontal = 4.dp))
        Column(Modifier.fillMaxWidth().selectableGroup()) {
            options.forEach { option ->
                val selected = option.mode.ordinal + 1 == current
                Row(
                    Modifier.fillMaxWidth().selectable(
                        selected = selected, role = Role.RadioButton,
                        onClick = { onSelectMode(option.mode) },
                    ).heightIn(min = 52.dp).padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ListeningModeGlyph(option.mode, colors.onSurfaceVariant)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(option.label), style = MaterialTheme.typography.bodyLarge)
                        if (selected) {
                            Text(stringResource(option.description), color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    RadioButton(selected = selected, onClick = null)
                }
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
