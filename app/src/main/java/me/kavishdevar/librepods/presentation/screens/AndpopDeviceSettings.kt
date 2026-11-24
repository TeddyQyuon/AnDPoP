// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager.Companion.ControlCommandIdentifiers
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.data.StemAction
import me.kavishdevar.librepods.presentation.components.AndpopGroup
import me.kavishdevar.librepods.presentation.components.AndpopRow
import me.kavishdevar.librepods.presentation.components.AndpopSectionHeader
import me.kavishdevar.librepods.presentation.components.AndpopSwitchRow
import me.kavishdevar.librepods.presentation.theme.AndpopIcons
import me.kavishdevar.librepods.presentation.theme.AndpopType
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsUiState

internal enum class DashboardDeviceControl {
    Rename,
    Microphone,
    LeftStem,
    RightStem,
    Accessibility,
    Firmware,
    Advanced,
}

@Composable
internal fun DashboardDeviceSettings(
    state: AirPodsUiState,
    onWearDetectionChanged: (Boolean) -> Unit,
    onConversationAwarenessChanged: (Boolean) -> Unit,
    onOpenControl: (DashboardDeviceControl) -> Unit,
) {
    val hasConversationAwareness = Capability.CONVERSATION_AWARENESS in state.capabilities && state.isPremium
    if (hasConversationAwareness) {
        AndpopSectionHeader(stringResource(R.string.andpop_sound))
        val enabled = state.controlStates[ControlCommandIdentifiers.CONVERSATION_DETECT_CONFIG]?.firstOrNull() == 1.toByte()
        AndpopGroup {
            row { shape ->
                AndpopSwitchRow(
                    shape = shape,
                    title = stringResource(R.string.conversation_awareness),
                    supporting = stringResource(R.string.andpop_conversation_description),
                    checked = enabled,
                    onCheckedChange = onConversationAwarenessChanged,
                    icon = AndpopIcons.Chat,
                    iconChip = true,
                )
            }
        }
    }

    AndpopSectionHeader(stringResource(R.string.andpop_controls_group))
    val microphone = state.controlStates[ControlCommandIdentifiers.MIC_MODE]?.firstOrNull()?.toInt()
    AndpopGroup {
        row { shape ->
            AndpopRow(
                shape, stringResource(R.string.name), value = state.deviceName, icon = AndpopIcons.Edit,
                onClick = { onOpenControl(DashboardDeviceControl.Rename) },
            )
        }
        row { shape ->
            AndpopSwitchRow(
                shape = shape,
                title = stringResource(R.string.andpop_ear_detection),
                checked = state.automaticEarDetectionEnabled,
                onCheckedChange = onWearDetectionChanged,
                icon = AndpopIcons.Ear,
            )
        }
        row { shape ->
            AndpopRow(
                shape, stringResource(R.string.andpop_microphone),
                value = stringResource(microphoneModeLabel(microphone)), icon = AndpopIcons.Mic,
                onClick = { onOpenControl(DashboardDeviceControl.Microphone) },
            )
        }
        row { shape ->
            AndpopRow(
                shape, stringResource(R.string.accessibility), icon = AndpopIcons.Accessibility,
                onClick = { onOpenControl(DashboardDeviceControl.Accessibility) },
            )
        }
    }

    if (Capability.STEM_CONFIG in state.capabilities) {
        AndpopSectionHeader(stringResource(R.string.andpop_press_and_hold))
        val stacked = LocalDensity.current.fontScale >= 1.6f
        val left: @Composable (Modifier) -> Unit = { modifier ->
            StemTile("L", stringResource(R.string.andpop_left), stemActionLabel(state.leftAction), modifier) {
                onOpenControl(DashboardDeviceControl.LeftStem)
            }
        }
        val right: @Composable (Modifier) -> Unit = { modifier ->
            StemTile("R", stringResource(R.string.andpop_right), stemActionLabel(state.rightAction), modifier) {
                onOpenControl(DashboardDeviceControl.RightStem)
            }
        }
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                left(Modifier.fillMaxWidth())
                right(Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                left(Modifier.weight(1f))
                right(Modifier.weight(1f))
            }
        }
    }

    AndpopSectionHeader(stringResource(R.string.andpop_about_airpods))
    AndpopGroup {
        if (state.actualModel.isNotBlank()) {
            row { shape -> AndpopRow(shape, stringResource(R.string.model_number), value = state.actualModel, icon = AndpopIcons.Info) }
        }
        if (state.version3.isNotBlank()) {
            row { shape ->
                AndpopRow(
                    shape, stringResource(R.string.andpop_firmware), value = state.version3, icon = AndpopIcons.Chip,
                    onClick = { onOpenControl(DashboardDeviceControl.Firmware) },
                )
            }
        }
        row { shape ->
            AndpopRow(
                shape, stringResource(R.string.andpop_all_device_settings),
                supporting = stringResource(R.string.andpop_all_device_settings_detail), icon = AndpopIcons.Grid,
                onClick = { onOpenControl(DashboardDeviceControl.Advanced) },
            )
        }
    }
}

/** One press-and-hold tile with an L or R badge and the current action. */
@Composable
private fun StemTile(badge: String, title: String, action: String?, modifier: Modifier, onClick: () -> Unit) {
    val palette = LocalAndpopPalette.current
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.heightIn(min = 112.dp).clip(shape).background(palette.s1).border(1.dp, palette.on.copy(alpha = 0.07f), shape)
            .clickable(role = Role.Button, onClick = onClick).padding(16.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(11.dp)).background(palette.pc),
                contentAlignment = Alignment.Center,
            ) {
                Text(badge, color = palette.onPc, style = AndpopType.label)
            }
            Box(Modifier.weight(1f))
            Icon(AndpopIcons.ChevronRight, null, Modifier.size(20.dp), tint = palette.on3)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = palette.on, style = AndpopType.titleSmall)
            if (!action.isNullOrBlank()) Text(action, color = palette.on2, style = AndpopType.bodySmall)
        }
    }
}

@Composable
private fun stemActionLabel(action: StemAction): String? = when (action) {
    StemAction.CYCLE_NOISE_CONTROL_MODES -> stringResource(R.string.listening_mode)
    StemAction.DIGITAL_ASSISTANT -> stringResource(R.string.digital_assistant)
    else -> null
}
