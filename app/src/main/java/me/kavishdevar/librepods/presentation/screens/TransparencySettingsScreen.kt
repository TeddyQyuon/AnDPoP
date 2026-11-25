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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.TransparencySettings
import me.kavishdevar.librepods.data.parseTransparencySettingsResponse
import me.kavishdevar.librepods.data.sendTransparencySettings
import me.kavishdevar.librepods.presentation.components.AndpopCard
import me.kavishdevar.librepods.presentation.components.AndpopDetailPage
import me.kavishdevar.librepods.presentation.components.AndpopGroup
import me.kavishdevar.librepods.presentation.components.AndpopSectionHeader
import me.kavishdevar.librepods.presentation.components.AndpopSliderRow
import me.kavishdevar.librepods.presentation.components.AndpopSwitchRow
import me.kavishdevar.librepods.presentation.components.InfoBanner
import me.kavishdevar.librepods.presentation.theme.AndpopIcons
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.roundToInt

@ExperimentalHazeMaterialsApi
@OptIn(ExperimentalEncodingApi::class)
@Composable
fun TransparencySettingsScreen(viewModel: AirPodsViewModel) {
    val state by viewModel.uiState.collectAsState()

    val enabled = rememberSaveable { mutableStateOf(false) }
    val amplification = rememberSaveable { mutableFloatStateOf(0.5f) }
    val balance = rememberSaveable { mutableFloatStateOf(0.5f) }
    val tone = rememberSaveable { mutableFloatStateOf(0.5f) }
    val ambientNoiseReduction = rememberSaveable { mutableFloatStateOf(0.0f) }
    val conversationBoost = rememberSaveable { mutableStateOf(false) }
    val eq = rememberSaveable(
        saver = Saver(
            save = { it.value.toList() },
            restore = { mutableStateOf(it.toFloatArray()) },
        ),
    ) { mutableStateOf(FloatArray(8)) }
    val initialized = rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.transparencyData) {
        val parsed = parseTransparencySettingsResponse(data = state.transparencyData) ?: return@LaunchedEffect
        enabled.value = parsed.enabled
        amplification.floatValue = parsed.netAmplification
        balance.floatValue = parsed.balance
        tone.floatValue = parsed.leftTone
        ambientNoiseReduction.floatValue = parsed.leftAmbientNoiseReduction
        conversationBoost.value = parsed.leftConversationBoost
        if (!eq.value.contentEquals(parsed.leftEQ)) {
            eq.value = parsed.leftEQ.copyOf()
        }
        initialized.value = true
    }

    LaunchedEffect(
        enabled.value,
        amplification.floatValue,
        balance.floatValue,
        tone.floatValue,
        conversationBoost.value,
        ambientNoiseReduction.floatValue,
        eq.value,
    ) {
        if (!initialized.value) return@LaunchedEffect
        val settings = TransparencySettings(
            enabled = enabled.value,
            leftEQ = eq.value,
            rightEQ = eq.value,
            leftAmplification = amplification.floatValue + if (balance.floatValue < 0) -balance.floatValue else 0f,
            rightAmplification = amplification.floatValue + if (balance.floatValue > 0) balance.floatValue else 0f,
            leftTone = tone.floatValue,
            rightTone = tone.floatValue,
            leftConversationBoost = conversationBoost.value,
            rightConversationBoost = conversationBoost.value,
            leftAmbientNoiseReduction = ambientNoiseReduction.floatValue,
            rightAmbientNoiseReduction = ambientNoiseReduction.floatValue,
            netAmplification = amplification.floatValue,
            balance = balance.floatValue,
        )
        sendTransparencySettings(viewModel::setATTCharacteristicValue, settings)
    }

    AndpopDetailPage(
        title = stringResource(R.string.customize_transparency_mode),
        subtitle = stringResource(R.string.andpop_transparency_subtitle),
    ) {
        if (!state.vendorIdHook) {
            InfoBanner(
                text = stringResource(R.string.act_as_an_apple_device_description),
                icon = AndpopIcons.Info,
            )
            return@AndpopDetailPage
        }

        AndpopGroup {
            row { shape ->
                AndpopSwitchRow(
                    shape = shape,
                    title = stringResource(R.string.transparency_mode),
                    supporting = stringResource(R.string.customize_transparency_mode_description),
                    checked = enabled.value,
                    onCheckedChange = { enabled.value = it },
                    icon = AndpopIcons.ModeTransparency,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        val ampPercent = (((amplification.floatValue + 1f) / 2f) * 100f).roundToInt().coerceIn(0, 100)
        val balanceDelta = (balance.floatValue * 100f).roundToInt().coerceIn(-100, 100)
        val balanceText = when {
            balanceDelta == 0 -> stringResource(R.string.andpop_balance_centered)
            balanceDelta < 0 -> stringResource(R.string.andpop_balance_left, -balanceDelta)
            else -> stringResource(R.string.andpop_balance_right, balanceDelta)
        }
        val toneText = stringResource(R.string.andpop_eq_step, (tone.floatValue * 100f).roundToInt().coerceIn(-100, 100))
        val nrPercent = (ambientNoiseReduction.floatValue * 100f).roundToInt().coerceIn(0, 100)

        AndpopCard {
            AndpopSliderRow(
                title = stringResource(R.string.amplification),
                valueText = stringResource(R.string.andpop_percent_value, ampPercent),
                value = amplification.floatValue,
                onValueChange = { amplification.floatValue = it },
                valueRange = -1f..1f,
                startIcon = AndpopIcons.SpeakerLow,
                endIcon = AndpopIcons.SpeakerHigh,
            )
            AndpopSliderRow(
                title = stringResource(R.string.balance),
                valueText = balanceText,
                value = balance.floatValue,
                onValueChange = { balance.floatValue = it },
                valueRange = -1f..1f,
                startLabel = stringResource(R.string.left),
                endLabel = stringResource(R.string.right),
            )
            AndpopSliderRow(
                title = stringResource(R.string.tone),
                valueText = toneText,
                value = tone.floatValue,
                onValueChange = { tone.floatValue = it },
                valueRange = -1f..1f,
                startLabel = stringResource(R.string.darker),
                endLabel = stringResource(R.string.brighter),
            )
            AndpopSliderRow(
                title = stringResource(R.string.ambient_noise_reduction),
                valueText = stringResource(R.string.andpop_percent_value, nrPercent),
                value = ambientNoiseReduction.floatValue,
                onValueChange = { ambientNoiseReduction.floatValue = it },
                valueRange = 0f..1f,
                startLabel = stringResource(R.string.less),
                endLabel = stringResource(R.string.more),
            )
        }

        Spacer(Modifier.height(12.dp))

        AndpopGroup {
            row { shape ->
                AndpopSwitchRow(
                    shape = shape,
                    title = stringResource(R.string.conversation_boost),
                    supporting = stringResource(R.string.andpop_conversation_boost_desc),
                    checked = conversationBoost.value,
                    onCheckedChange = { conversationBoost.value = it },
                    icon = AndpopIcons.Chat,
                )
            }
        }

        AndpopSectionHeader(stringResource(R.string.equalizer))
        AndpopCard {
            for (i in 0 until 8) {
                AndpopSliderRow(
                    title = stringResource(R.string.band_label, i + 1),
                    valueText = stringResource(R.string.andpop_percent_value, eq.value[i].roundToInt().coerceIn(0, 100)),
                    value = eq.value[i],
                    onValueChange = { newVal ->
                        val updated = eq.value.copyOf()
                        updated[i] = newVal
                        eq.value = updated
                    },
                    valueRange = 0f..100f,
                )
            }
        }
    }
}
