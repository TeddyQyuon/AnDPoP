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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.BuildConfig
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager.Companion.ControlCommandIdentifiers
import me.kavishdevar.librepods.bluetooth.ATTHandles
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.presentation.components.AndpopCard
import me.kavishdevar.librepods.presentation.components.AndpopDetailPage
import me.kavishdevar.librepods.presentation.components.AndpopGroup
import me.kavishdevar.librepods.presentation.components.AndpopRow
import me.kavishdevar.librepods.presentation.components.AndpopSectionHeader
import me.kavishdevar.librepods.presentation.components.AndpopSegmentedRow
import me.kavishdevar.librepods.presentation.components.AndpopSliderRow
import me.kavishdevar.librepods.presentation.components.AndpopSwitchRow
import me.kavishdevar.librepods.presentation.theme.AndpopIcons
import me.kavishdevar.librepods.presentation.theme.AndpopType
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import kotlin.math.roundToInt

/** Byte values for the three press-speed and hold-duration steps. */
private val PRESS_VALUES = listOf<Byte>(0, 1, 2)

/** Volume swipe interval values start at 1. */
private val SWIPE_VALUES = listOf<Byte>(1, 2, 3)

@Composable
fun AccessibilitySettingsScreen(viewModel: AirPodsViewModel, navigateToTransparencyCustomization: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val palette = LocalAndpopPalette.current
    val haptics = LocalHapticFeedback.current
    val controls = state.controlStates
    val hearingAid = controls[ControlCommandIdentifiers.HEARING_AID]
    val hearingAidEnabled = hearingAid?.getOrNull(0)?.toInt() == 1 && hearingAid.getOrNull(1)?.toInt() == 1
    val swipeSupported = Capability.SWIPE_FOR_VOLUME in state.capabilities

    fun pick(identifier: ControlCommandIdentifiers, value: Byte) {
        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        viewModel.setControlCommandByte(identifier, value)
    }

    AndpopDetailPage(title = stringResource(R.string.accessibility)) {
        val slow = listOf(stringResource(R.string.default_option), stringResource(R.string.slower), stringResource(R.string.slowest))

        AndpopSectionHeader(stringResource(R.string.press_speed))
        AndpopCard {
            Column(Modifier.padding(12.dp)) {
                AndpopSegmentedRow(
                    options = slow,
                    selectedIndex = PRESS_VALUES.indexOf(controls[ControlCommandIdentifiers.DOUBLE_CLICK_INTERVAL]?.getOrNull(0)).coerceAtLeast(0),
                    onSelect = { pick(ControlCommandIdentifiers.DOUBLE_CLICK_INTERVAL, PRESS_VALUES[it]) },
                )
                Text(
                    stringResource(R.string.andpop_press_speed_desc),
                    Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 2.dp),
                    color = palette.on2,
                    style = AndpopType.bodySmall,
                )
            }
        }

        AndpopSectionHeader(stringResource(R.string.press_and_hold_duration))
        AndpopCard {
            Column(Modifier.padding(12.dp)) {
                AndpopSegmentedRow(
                    options = slow,
                    selectedIndex = PRESS_VALUES.indexOf(controls[ControlCommandIdentifiers.CLICK_HOLD_INTERVAL]?.getOrNull(0)).coerceAtLeast(0),
                    onSelect = { pick(ControlCommandIdentifiers.CLICK_HOLD_INTERVAL, PRESS_VALUES[it]) },
                )
            }
        }

        if (swipeSupported) {
            AndpopSectionHeader(stringResource(R.string.volume_swipe_speed))
            AndpopCard {
                Column(Modifier.padding(12.dp)) {
                    AndpopSegmentedRow(
                        options = listOf(stringResource(R.string.default_option), stringResource(R.string.longer), stringResource(R.string.longest)),
                        selectedIndex = SWIPE_VALUES.indexOf(controls[ControlCommandIdentifiers.VOLUME_SWIPE_INTERVAL]?.getOrNull(0)).coerceAtLeast(0),
                        onSelect = { pick(ControlCommandIdentifiers.VOLUME_SWIPE_INTERVAL, SWIPE_VALUES[it]) },
                    )
                }
            }
        }

        AndpopSectionHeader(stringResource(R.string.listening_mode))
        AndpopGroup {
            row { shape ->
                AndpopSwitchRow(
                    shape = shape,
                    title = stringResource(R.string.noise_cancellation_single_airpod),
                    supporting = stringResource(R.string.andpop_one_bud_anc_desc),
                    checked = controls[ControlCommandIdentifiers.ONE_BUD_ANC_MODE]?.getOrNull(0) == 0x01.toByte(),
                    onCheckedChange = { viewModel.setControlCommandBoolean(ControlCommandIdentifiers.ONE_BUD_ANC_MODE, it) },
                )
            }
            // Loud sound reduction and Transparency customization are written over ATT, which needs Xposed.
            if (Capability.LOUD_SOUND_REDUCTION in state.capabilities && state.vendorIdHook && !BuildConfig.PLAY_BUILD) {
                row { shape ->
                    AndpopSwitchRow(
                        shape = shape,
                        title = stringResource(R.string.loud_sound_reduction),
                        checked = state.loudSoundReductionEnabled,
                        onCheckedChange = {
                            viewModel.setATTCharacteristicValue(ATTHandles.LOUD_SOUND_REDUCTION, byteArrayOf(if (it) 0x01 else 0x00))
                        },
                    )
                }
            }
            if (!hearingAidEnabled && state.vendorIdHook && !BuildConfig.PLAY_BUILD) {
                row { shape ->
                    AndpopRow(
                        shape = shape,
                        title = stringResource(R.string.customize_transparency_mode),
                        onClick = navigateToTransparencyCustomization,
                    )
                }
            }
        }

        AndpopSectionHeader(stringResource(R.string.andpop_sounds_and_swipes))
        val storedTone = controls[ControlCommandIdentifiers.CHIME_VOLUME]?.getOrNull(0)?.toInt()?.and(0xFF) ?: 75
        var tone by remember(storedTone) { mutableFloatStateOf(storedTone.toFloat()) }
        AndpopGroup {
            row { shape ->
                AndpopCard(shape = shape) {
                    AndpopSliderRow(
                        title = stringResource(R.string.tone_volume),
                        valueText = stringResource(R.string.andpop_percent_value, tone.roundToInt()),
                        value = tone,
                        onValueChange = { tone = it },
                        onValueChangeFinished = {
                            viewModel.setControlCommandValue(
                                ControlCommandIdentifiers.CHIME_VOLUME,
                                byteArrayOf(tone.roundToInt().toByte(), 0x50),
                            )
                        },
                        valueRange = 0f..100f,
                        startIcon = AndpopIcons.SpeakerLow,
                        endIcon = AndpopIcons.SpeakerHigh,
                    )
                }
            }
            if (swipeSupported) {
                row { shape ->
                    AndpopSwitchRow(
                        shape = shape,
                        title = stringResource(R.string.volume_control),
                        supporting = stringResource(R.string.andpop_swipe_volume_desc),
                        checked = controls[ControlCommandIdentifiers.VOLUME_SWIPE_MODE]?.getOrNull(0)?.toInt() == 0x01,
                        onCheckedChange = { viewModel.setControlCommandBoolean(ControlCommandIdentifiers.VOLUME_SWIPE_MODE, it) },
                    )
                }
            }
        }
    }
}
