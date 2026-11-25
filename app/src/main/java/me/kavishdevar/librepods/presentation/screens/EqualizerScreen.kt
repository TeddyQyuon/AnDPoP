/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package me.kavishdevar.librepods.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.AndpopCard
import me.kavishdevar.librepods.presentation.components.AndpopDetailPage
import me.kavishdevar.librepods.presentation.components.AndpopFootnote
import me.kavishdevar.librepods.presentation.components.AndpopGroup
import me.kavishdevar.librepods.presentation.components.AndpopSegmentedRow
import me.kavishdevar.librepods.presentation.components.AndpopSliderRow
import me.kavishdevar.librepods.presentation.components.AndpopTextButton
import me.kavishdevar.librepods.presentation.theme.AndpopType
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsUiState
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import kotlin.math.roundToInt

/** The sliders show six steps either side of flat; the AirPods store 0–100 with 50 as flat. */
internal const val EQ_STEPS = 6

internal fun eqStepFromValue(value: Int): Int = ((value.coerceIn(0, 100) - 50) * EQ_STEPS / 50f).roundToInt()

internal fun eqValueFromStep(step: Int): Int = (50 + step.coerceIn(-EQ_STEPS, EQ_STEPS) * 50f / EQ_STEPS).roundToInt()

@Composable
fun EqualizerRoute(viewModel: AirPodsViewModel) {
    val state by viewModel.uiState.collectAsState()
    EqualizerScreen(
        state = state,
        setCustomEqEnabled = viewModel::setCustomEqEnabled,
        setCustomEq = viewModel::setCustomEq,
    )
}

@Composable
fun EqualizerScreen(
    state: AirPodsUiState,
    setCustomEqEnabled: (Boolean) -> Unit,
    setCustomEq: (Int, Int, Int) -> Unit,
) {
    val eq = state.customEq
    val custom = eq.isEnabled()
    var low by remember(eq.low) { mutableIntStateOf(eqStepFromValue(eq.low)) }
    var mid by remember(eq.mid) { mutableIntStateOf(eqStepFromValue(eq.mid)) }
    var high by remember(eq.high) { mutableIntStateOf(eqStepFromValue(eq.high)) }

    fun send() = setCustomEq(eqValueFromStep(low), eqValueFromStep(mid), eqValueFromStep(high))

    AndpopDetailPage(title = stringResource(R.string.equalizer)) {
        Spacer(Modifier.height(16.dp))
        AndpopSegmentedRow(
            options = listOf(stringResource(R.string.recommended), stringResource(R.string.custom)),
            selectedIndex = if (custom) 1 else 0,
            onSelect = { setCustomEqEnabled(it == 1) },
        )
        EqCurve(low, mid, high, Modifier.padding(top = 12.dp).alpha(if (custom) 1f else 0.55f))
        if (custom) {
            val bands = listOf(
                Triple(R.string.andpop_eq_low, low) { v: Int -> low = v },
                Triple(R.string.andpop_eq_mid, mid) { v: Int -> mid = v },
                Triple(R.string.andpop_eq_high, high) { v: Int -> high = v },
            )
            AndpopGroup(Modifier.padding(top = 12.dp)) {
                bands.forEach { (label, step, update) ->
                    row { shape ->
                        AndpopCard(shape = shape) {
                            AndpopSliderRow(
                                title = stringResource(label),
                                valueText = stringResource(R.string.andpop_eq_step, step),
                                value = step.toFloat(),
                                onValueChange = { update(it.roundToInt()) },
                                onValueChangeFinished = ::send,
                                valueRange = -EQ_STEPS.toFloat()..EQ_STEPS.toFloat(),
                                steps = EQ_STEPS * 2 - 1,
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                AndpopTextButton(
                    stringResource(R.string.reset),
                    onClick = {
                        low = 0; mid = 0; high = 0
                        send()
                    },
                    enabled = low != 0 || mid != 0 || high != 0,
                )
            }
        } else {
            AndpopFootnote(stringResource(R.string.andpop_eq_recommended_note), Modifier.padding(top = 4.dp))
        }
    }
}

/** Smooth curve through the three bands, as in the prototype. Decorative; the sliders carry the values. */
@Composable
private fun EqCurve(low: Int, mid: Int, high: Int, modifier: Modifier = Modifier) {
    val palette = LocalAndpopPalette.current
    AndpopCard(modifier) {
        Canvas(Modifier.fillMaxWidth().height(150.dp).padding(horizontal = 12.dp, vertical = 10.dp).clearAndSetSemantics {}) {
            val w = size.width
            val h = size.height
            fun y(step: Int) = h / 2 - step * (h / 2 - 8.dp.toPx()) / EQ_STEPS
            val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx()))
            drawLine(palette.line2, Offset(0f, h / 2), Offset(w, h / 2), 1.dp.toPx(), pathEffect = dash)
            drawLine(palette.line, Offset(w / 2, 0f), Offset(w / 2, h), 1.dp.toPx())
            drawLine(palette.line, Offset(0f, 0f), Offset(w, 0f), 1.dp.toPx())
            drawLine(palette.line, Offset(0f, h), Offset(w, h), 1.dp.toPx())
            val curve = Path().apply {
                moveTo(0f, y(low))
                cubicTo(w / 6, y(low), w * 0.27f, y(mid), w / 2, y(mid))
                cubicTo(w * 0.73f, y(mid), w * 5 / 6, y(high), w, y(high))
            }
            val fill = Path().apply {
                addPath(curve)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(fill, palette.pri.copy(alpha = 0.16f))
            drawPath(curve, palette.pri, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(R.string.andpop_eq_low, R.string.andpop_eq_mid, R.string.andpop_eq_high).forEach {
                Text(stringResource(it).uppercase(), color = palette.on3, style = AndpopType.tiny)
            }
        }
    }
}
