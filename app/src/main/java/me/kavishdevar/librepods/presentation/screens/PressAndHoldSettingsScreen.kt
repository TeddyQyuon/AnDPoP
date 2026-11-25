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

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.data.StemAction
import me.kavishdevar.librepods.presentation.components.AndpopCheckboxRow
import me.kavishdevar.librepods.presentation.components.AndpopDetailPage
import me.kavishdevar.librepods.presentation.components.AndpopFootnote
import me.kavishdevar.librepods.presentation.components.AndpopGroup
import me.kavishdevar.librepods.presentation.components.AndpopRadioRow
import me.kavishdevar.librepods.presentation.components.AndpopSectionHeader
import me.kavishdevar.librepods.presentation.theme.AndpopIcons
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel

/** A listening mode that press and hold can cycle through, with its bit in LISTENING_MODE_CONFIGS. */
internal enum class CycleMode(val bit: Int, @param:StringRes val label: Int, @param:StringRes val description: Int) {
    Off(0x01, R.string.off, R.string.listening_mode_off_description),
    Transparency(0x04, R.string.transparency, R.string.listening_mode_transparency_description),
    Adaptive(0x08, R.string.adaptive, R.string.listening_mode_adaptive_description),
    NoiseCancellation(0x02, R.string.noise_cancellation, R.string.listening_mode_noise_cancellation_description),
}

private fun CycleMode.icon(): ImageVector = when (this) {
    CycleMode.Off -> AndpopIcons.ModeOff
    CycleMode.Transparency -> AndpopIcons.ModeTransparency
    CycleMode.Adaptive -> AndpopIcons.ModeAdaptive
    CycleMode.NoiseCancellation -> AndpopIcons.ModeNoiseCancellation
}

/** True when unchecking [mode] would leave fewer than two modes in the cycle. */
internal fun isCycleModeLocked(modesByte: Int, mode: CycleMode): Boolean =
    (modesByte and mode.bit) != 0 && Integer.bitCount(modesByte and 0x0F) <= 2

@Composable
fun LongPress(viewModel: AirPodsViewModel, name: String) {
    val state by viewModel.uiState.collectAsState()
    val palette = LocalAndpopPalette.current
    val haptics = LocalHapticFeedback.current
    val left = name.equals("left", ignoreCase = true)
    val action = if (left) state.leftAction else state.rightAction
    val modesByte = state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE_CONFIGS]
        ?.getOrNull(0)?.toInt()?.and(0xFF) ?: 0
    val modes = CycleMode.entries.filter { mode ->
        when (mode) {
            CycleMode.Off -> state.offListeningMode
            CycleMode.Adaptive -> state.capabilities.isEmpty() || Capability.ADAPTIVE_AUDIO in state.capabilities
            else -> true
        }
    }

    // Unchecking one of the last two modes shakes its checkbox and turns the rule red.
    var rejectedMode by remember { mutableStateOf<CycleMode?>(null) }
    var rejections by remember { mutableIntStateOf(0) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(rejections) {
        if (rejections == 0) return@LaunchedEffect
        shake.snapTo(0f)
        shake.animateTo(0f, keyframes {
            durationMillis = 360
            -8f at 50
            8f at 110
            -6f at 170
            6f at 230
            -3f at 290
        })
        delay(1200)
        rejectedMode = null
    }
    val ruleColor by animateColorAsState(if (rejectedMode != null) palette.error else palette.on2, label = "Cycle rule color")

    AndpopDetailPage(
        title = stringResource(if (left) R.string.andpop_left_bud else R.string.andpop_right_bud),
        subtitle = stringResource(R.string.andpop_press_hold_subtitle),
    ) {
        Spacer(Modifier.height(16.dp))
        AndpopGroup(Modifier.selectableGroup()) {
            row { shape ->
                AndpopRadioRow(
                    shape = shape,
                    title = stringResource(R.string.andpop_switch_listening_mode),
                    icon = AndpopIcons.Layers,
                    selected = action == StemAction.CYCLE_NOISE_CONTROL_MODES,
                    onClick = { viewModel.setLongPressAction(name, StemAction.CYCLE_NOISE_CONTROL_MODES) },
                )
            }
            row { shape ->
                AndpopRadioRow(
                    shape = shape,
                    title = stringResource(R.string.digital_assistant),
                    supporting = stringResource(R.string.andpop_digital_assistant_desc),
                    icon = AndpopIcons.Sparkle,
                    selected = action == StemAction.DIGITAL_ASSISTANT,
                    onClick = { viewModel.setLongPressAction(name, StemAction.DIGITAL_ASSISTANT) },
                )
            }
        }

        if (action == StemAction.CYCLE_NOISE_CONTROL_MODES) {
            AndpopSectionHeader(stringResource(R.string.andpop_cycle_between))
            AndpopGroup {
                modes.forEach { mode ->
                    row { shape ->
                        val checked = (modesByte and mode.bit) != 0
                        val locked = isCycleModeLocked(modesByte, mode)
                        AndpopCheckboxRow(
                            shape = shape,
                            title = stringResource(mode.label),
                            supporting = stringResource(mode.description),
                            icon = mode.icon(),
                            checked = checked,
                            locked = locked,
                            checkboxModifier = if (rejectedMode == mode) Modifier.offset(x = shake.value.dp) else Modifier,
                            onClick = {
                                if (locked) {
                                    rejectedMode = mode
                                    rejections++
                                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                                } else {
                                    viewModel.toggleListeningMode(mode.bit)
                                    haptics.performHapticFeedback(if (checked) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                                }
                            },
                        )
                    }
                }
            }
            AndpopFootnote(
                stringResource(R.string.andpop_cycle_two_rule),
                Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = ruleColor,
            )
        }
    }
}
