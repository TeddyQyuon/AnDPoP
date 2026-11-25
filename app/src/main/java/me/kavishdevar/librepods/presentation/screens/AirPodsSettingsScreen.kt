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

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.BuildConfig
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.bluetooth.ATTHandles
import me.kavishdevar.librepods.data.AirPodsPro3
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.presentation.components.AndpopGroup
import me.kavishdevar.librepods.presentation.components.AndpopPageHeader
import me.kavishdevar.librepods.presentation.components.AndpopRow
import me.kavishdevar.librepods.presentation.components.AndpopSectionHeader
import me.kavishdevar.librepods.presentation.components.AndpopSwitchRow
import me.kavishdevar.librepods.presentation.components.StatusPill
import me.kavishdevar.librepods.presentation.theme.AndpopIcons
import me.kavishdevar.librepods.presentation.theme.AndpopType
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsUiState
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel

@Composable
fun AirPodsSettingsRoute(
    viewModel: AirPodsViewModel,
    navigateToHearingProtection: () -> Unit,
    navigateToHearingAid: () -> Unit,
    navigateToAdaptiveStrength: () -> Unit,
    navigateToEqualizer: () -> Unit,
    navigateToHeadTracking: () -> Unit,
    navigateToVersion: () -> Unit,
    navigateToCallControlScreen: (action: String) -> Unit,
    onDisconnected: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    AirPodsSettingsScreen(
        state = state,
        setControlCommandBoolean = viewModel::setControlCommandBoolean,
        setATTCharacteristicValue = viewModel::setATTCharacteristicValue,
        onAutomaticEarDetectionChanged = viewModel::setAutomaticEarDetectionEnabled,
        onAutomaticConnectionChanged = viewModel::setAutomaticConnectionEnabled,
        setDynamicEndOfCharge = viewModel::setDynamicEndOfCharge,
        setOffListeningMode = viewModel::setOffListeningMode,
        disconnect = {
            viewModel.disconnect()
            onDisconnected()
        },
        navigateToHearingProtection = navigateToHearingProtection,
        navigateToHearingAid = navigateToHearingAid,
        navigateToAdaptiveStrength = navigateToAdaptiveStrength,
        navigateToEqualizer = navigateToEqualizer,
        navigateToHeadTracking = navigateToHeadTracking,
        navigateToVersion = navigateToVersion,
        navigateToCallControlScreen = navigateToCallControlScreen,
    )
}

@SuppressLint("MissingPermission")
@Composable
internal fun AirPodsSettingsScreen(
    state: AirPodsUiState,
    setControlCommandBoolean: (AACPManager.Companion.ControlCommandIdentifiers, Boolean) -> Unit,
    setATTCharacteristicValue: (ATTHandles, ByteArray) -> Unit,
    onAutomaticEarDetectionChanged: (Boolean) -> Unit,
    onAutomaticConnectionChanged: (Boolean) -> Unit,
    setDynamicEndOfCharge: (Boolean) -> Unit,
    setOffListeningMode: (Boolean) -> Unit,
    disconnect: () -> Unit,
    navigateToHearingProtection: () -> Unit,
    navigateToHearingAid: () -> Unit,
    navigateToAdaptiveStrength: () -> Unit,
    navigateToEqualizer: () -> Unit,
    navigateToHeadTracking: () -> Unit,
    navigateToVersion: () -> Unit,
    navigateToCallControlScreen: (action: String) -> Unit,
) {
    val palette = LocalAndpopPalette.current
    val capabilities = state.capabilities
    var serialIndex by rememberSaveable { mutableIntStateOf(0) }
    var showDisconnectDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        AndpopPageHeader(
            title = stringResource(R.string.andpop_all_device_settings),
            subtitle = state.deviceName.ifBlank { state.modelName },
        )

        // 1. Audio
        AndpopSectionHeader(stringResource(R.string.audio))
        val adaptiveVolumeEnabled = state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.ADAPTIVE_VOLUME_CONFIG]?.getOrNull(0) == 0x01.toByte()
        val conversationEnabled = state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.CONVERSATION_DETECT_CONFIG]?.getOrNull(0) == 0x01.toByte()
        val adaptiveStrength = 100 - (state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.AUTO_ANC_STRENGTH]?.firstOrNull()?.toInt() ?: 50)
        val eqLabel = stringResource(if (state.customEq.isEnabled()) R.string.custom else R.string.recommended)
        AndpopGroup {
            if (Capability.ADAPTIVE_VOLUME in capabilities) {
                row { shape ->
                    AndpopSwitchRow(
                        shape = shape,
                        title = stringResource(R.string.personalized_volume),
                        supporting = stringResource(R.string.andpop_personalized_volume_desc),
                        checked = adaptiveVolumeEnabled,
                        onCheckedChange = {
                            setControlCommandBoolean(AACPManager.Companion.ControlCommandIdentifiers.ADAPTIVE_VOLUME_CONFIG, it)
                        },
                        icon = AndpopIcons.SpeakerHigh,
                    )
                }
            }
            if (Capability.CONVERSATION_AWARENESS in capabilities) {
                row { shape ->
                    AndpopSwitchRow(
                        shape = shape,
                        title = stringResource(R.string.conversation_awareness),
                        checked = conversationEnabled,
                        onCheckedChange = {
                            setControlCommandBoolean(AACPManager.Companion.ControlCommandIdentifiers.CONVERSATION_DETECT_CONFIG, it)
                        },
                        icon = AndpopIcons.Chat,
                    )
                }
            }
            if (Capability.LOUD_SOUND_REDUCTION in capabilities && state.vendorIdHook && !BuildConfig.PLAY_BUILD) {
                row { shape ->
                    AndpopSwitchRow(
                        shape = shape,
                        title = stringResource(R.string.loud_sound_reduction),
                        checked = state.loudSoundReductionEnabled,
                        onCheckedChange = {
                            setATTCharacteristicValue(ATTHandles.LOUD_SOUND_REDUCTION, byteArrayOf(if (it) 0x01 else 0x00))
                        },
                        icon = AndpopIcons.Shield,
                    )
                }
            }
            if (Capability.ADAPTIVE_AUDIO in capabilities) {
                row { shape ->
                    AndpopRow(
                        shape = shape,
                        title = stringResource(R.string.adaptive_audio),
                        value = stringResource(R.string.andpop_percent_value, adaptiveStrength),
                        icon = AndpopIcons.ModeAdaptive,
                        onClick = navigateToAdaptiveStrength,
                    )
                }
            }
            row { shape ->
                AndpopRow(
                    shape = shape,
                    title = stringResource(R.string.equalizer),
                    value = eqLabel,
                    icon = AndpopIcons.Equalizer,
                    onClick = navigateToEqualizer,
                )
            }
        }

        // 2. Hearing health
        val showHearingProtection = Capability.PPE in capabilities
        val showHearingAid = Capability.HEARING_AID in capabilities && state.vendorIdHook && !BuildConfig.PLAY_BUILD
        if (showHearingProtection || showHearingAid) {
            AndpopSectionHeader(stringResource(R.string.hearing_health))
            val hearingAidOn = state.hearingAidData.getOrNull(2)?.toInt() == 1
            AndpopGroup {
                if (showHearingProtection) {
                    row { shape ->
                        AndpopRow(
                            shape = shape,
                            title = stringResource(R.string.hearing_protection),
                            icon = AndpopIcons.Shield,
                            onClick = navigateToHearingProtection,
                        )
                    }
                }
                if (showHearingAid) {
                    row { shape ->
                        AndpopRow(
                            shape = shape,
                            title = stringResource(R.string.hearing_aid),
                            value = stringResource(if (hearingAidOn) R.string.on else R.string.off),
                            icon = AndpopIcons.Hearing,
                            onClick = navigateToHearingAid,
                        )
                    }
                }
            }
        }

        // 3. Calls
        val callConfig = state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.CALL_MANAGEMENT_CONFIG]
        val flipped = callControlsFlipped(callConfig)
        val muteValue = stringResource(if (flipped) R.string.andpop_press_twice else R.string.andpop_press_once)
        val hangValue = stringResource(if (flipped) R.string.andpop_press_once else R.string.andpop_press_twice)
        val muteActionName = stringResource(R.string.andpop_mute_unmute)
        val hangActionName = stringResource(R.string.andpop_hang_up)
        AndpopSectionHeader(stringResource(R.string.andpop_calls))
        AndpopGroup {
            row { shape ->
                AndpopRow(
                    shape = shape,
                    title = stringResource(R.string.answer_call),
                    value = stringResource(R.string.andpop_press_once),
                    icon = AndpopIcons.Phone,
                )
            }
            row { shape ->
                AndpopRow(
                    shape = shape,
                    title = muteActionName,
                    value = muteValue,
                    icon = AndpopIcons.Mic,
                    onClick = { navigateToCallControlScreen(CALL_ACTION_MUTE) },
                )
            }
            row { shape ->
                AndpopRow(
                    shape = shape,
                    title = hangActionName,
                    value = hangValue,
                    icon = AndpopIcons.Close,
                    onClick = { navigateToCallControlScreen(CALL_ACTION_HANG_UP) },
                )
            }
            if (Capability.HEAD_GESTURES in capabilities) {
                row { shape ->
                    AndpopRow(
                        shape = shape,
                        title = stringResource(R.string.head_gestures),
                        value = stringResource(if (state.headGesturesEnabled) R.string.on else R.string.off),
                        icon = AndpopIcons.Head,
                        onClick = navigateToHeadTracking,
                    )
                }
            }
        }

        // 4. Connection and charging
        val sleepEnabled = state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.SLEEP_DETECTION_CONFIG]?.getOrNull(0) == 0x01.toByte()
        AndpopSectionHeader(stringResource(R.string.andpop_connection_charging))
        AndpopGroup {
            row { shape ->
                AndpopSwitchRow(
                    shape = shape,
                    title = stringResource(R.string.andpop_ear_detection),
                    checked = state.automaticEarDetectionEnabled,
                    onCheckedChange = onAutomaticEarDetectionChanged,
                    icon = AndpopIcons.Ear,
                )
            }
            row { shape ->
                AndpopSwitchRow(
                    shape = shape,
                    title = stringResource(R.string.andpop_connect_automatically),
                    supporting = stringResource(R.string.andpop_auto_connect_desc),
                    checked = state.automaticConnectionEnabled,
                    onCheckedChange = onAutomaticConnectionChanged,
                    icon = AndpopIcons.Bluetooth,
                )
            }
            if (Capability.SLEEP_DETECTION in capabilities) {
                row { shape ->
                    AndpopSwitchRow(
                        shape = shape,
                        title = stringResource(R.string.sleep_detection),
                        checked = sleepEnabled,
                        onCheckedChange = {
                            setControlCommandBoolean(AACPManager.Companion.ControlCommandIdentifiers.SLEEP_DETECTION_CONFIG, it)
                        },
                        icon = AndpopIcons.Moon,
                    )
                }
            }
            if (state.instance?.model !is AirPodsPro3) {
                row { shape ->
                    AndpopSwitchRow(
                        shape = shape,
                        title = stringResource(R.string.andpop_charge_limit_title),
                        supporting = stringResource(R.string.andpop_charge_limit_desc),
                        checked = state.dynamicEndOfCharge,
                        onCheckedChange = setDynamicEndOfCharge,
                        icon = AndpopIcons.Battery,
                    )
                }
            }
            if (Capability.LISTENING_MODE in capabilities) {
                row { shape ->
                    AndpopSwitchRow(
                        shape = shape,
                        title = stringResource(R.string.off_listening_mode),
                        supporting = stringResource(R.string.andpop_off_mode_desc),
                        checked = state.offListeningMode,
                        onCheckedChange = setOffListeningMode,
                        icon = AndpopIcons.ModeOff,
                    )
                }
            }
        }

        // 5. About
        val serialSlots = listOf(
            stringResource(R.string.andpop_serial_case) to state.serialNumbers.getOrElse(0) { "" },
            stringResource(R.string.andpop_serial_left) to state.serialNumbers.getOrElse(1) { "" },
            stringResource(R.string.andpop_serial_right) to state.serialNumbers.getOrElse(2) { "" },
        ).filter { it.second.isNotBlank() }
        val activeSerial = if (serialSlots.isNotEmpty()) serialSlots[serialIndex % serialSlots.size] else null

        AndpopSectionHeader(stringResource(R.string.andpop_about_section))
        AndpopGroup {
            if (state.modelName.isNotBlank()) {
                row { shape ->
                    AndpopRow(shape = shape, title = stringResource(R.string.model_name), value = state.modelName)
                }
            }
            if (state.actualModel.isNotBlank()) {
                row { shape ->
                    AndpopRow(shape = shape, title = stringResource(R.string.model_number), value = state.actualModel)
                }
            }
            if (activeSerial != null) {
                row { shape ->
                    val serialTitle = stringResource(R.string.serial_number)
                    AndpopRow(
                        shape = shape,
                        title = serialTitle,
                        supporting = if (serialSlots.size > 1) stringResource(R.string.andpop_serial_tap_hint) else null,
                        value = activeSerial.second,
                        showChevron = false,
                        onClick = if (serialSlots.size > 1) ({ serialIndex++ }) else null,
                        modifier = Modifier.semantics { contentDescription = "$serialTitle, ${activeSerial.first}, ${activeSerial.second}" },
                        trailing = { StatusPill(activeSerial.first) },
                    )
                }
            }
            if (state.version3.isNotBlank()) {
                row { shape ->
                    AndpopRow(
                        shape = shape,
                        title = stringResource(R.string.version),
                        value = state.version3,
                        onClick = navigateToVersion,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = { showDisconnectDialog = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            border = BorderStroke(1.dp, palette.error.copy(alpha = 0.45f)),
        ) {
            Text(stringResource(R.string.disconnect), color = palette.error, style = AndpopType.button)
        }
        Spacer(Modifier.height(32.dp))
    }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            containerColor = palette.s2,
            icon = { Icon(AndpopIcons.Bluetooth, null, tint = palette.pri) },
            title = { Text(stringResource(R.string.andpop_disconnect_title), style = AndpopType.title) },
            text = { Text(stringResource(R.string.andpop_disconnect_body), color = palette.on2, style = AndpopType.bodySmall) },
            confirmButton = {
                TextButton(onClick = {
                    showDisconnectDialog = false
                    disconnect()
                }) {
                    Text(stringResource(R.string.disconnect), color = palette.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}
