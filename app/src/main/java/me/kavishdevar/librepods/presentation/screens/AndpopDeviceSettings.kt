// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.presentation.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager.Companion.ControlCommandIdentifiers
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.data.StemAction
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.presentation.theme.devicePanel
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
        ControlsGroup(R.string.andpop_sound) {
            val enabled = state.controlStates[ControlCommandIdentifiers.CONVERSATION_DETECT_CONFIG]?.firstOrNull() == 1.toByte()
            DeviceToggleRow(
                R.string.conversation_awareness, enabled, onConversationAwarenessChanged,
                description = R.string.andpop_conversation_description,
            )
        }
    }
    ControlsGroup(R.string.andpop_controls_group) {
        DeviceSettingRow(R.string.name, state.deviceName) { onOpenControl(DashboardDeviceControl.Rename) }
        SettingDivider()
        DeviceToggleRow(R.string.andpop_ear_detection, state.automaticEarDetectionEnabled, onWearDetectionChanged)
        val microphone = state.controlStates[ControlCommandIdentifiers.MIC_MODE]?.firstOrNull()?.toInt()
        SettingDivider()
        DeviceSettingRow(R.string.andpop_microphone, stringResource(microphoneModeLabel(microphone))) {
            onOpenControl(DashboardDeviceControl.Microphone)
        }
        SettingDivider()
        DeviceSettingRow(R.string.accessibility) { onOpenControl(DashboardDeviceControl.Accessibility) }
    }

    if (Capability.STEM_CONFIG in state.capabilities) {
        ControlsGroup(R.string.press_and_hold_airpods) {
            DeviceSettingRow(R.string.andpop_left, stemActionLabel(state.leftAction)) {
                onOpenControl(DashboardDeviceControl.LeftStem)
            }
            SettingDivider()
            DeviceSettingRow(R.string.andpop_right, stemActionLabel(state.rightAction)) {
                onOpenControl(DashboardDeviceControl.RightStem)
            }
        }
    }

    ControlsGroup(R.string.about) {
        if (state.actualModel.isNotBlank()) {
            DeviceSettingRow(R.string.model_number, state.actualModel)
            SettingDivider()
        }
        if (state.version3.isNotBlank()) {
            DeviceSettingRow(R.string.andpop_firmware, state.version3) {
                onOpenControl(DashboardDeviceControl.Firmware)
            }
            SettingDivider()
        }
        DeviceSettingRow(R.string.andpop_device_controls) { onOpenControl(DashboardDeviceControl.Advanced) }
    }
}

@Composable
private fun ControlsGroup(@StringRes label: Int, content: @Composable () -> Unit) {
    val palette = LocalAndpopPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DashboardHeading(stringResource(label), Modifier.padding(horizontal = 4.dp))
        Column(Modifier.fillMaxWidth().devicePanel(palette).padding(vertical = 3.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun DeviceSettingRow(@StringRes label: Int, detail: String? = null, onClick: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    val tap = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).then(tap).padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            stringResource(label), Modifier.weight(1f),
            color = colors.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (!detail.isNullOrBlank()) {
            Text(
                detail, Modifier.weight(0.82f), color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.End, overflow = TextOverflow.Ellipsis, maxLines = 2,
            )
        }
        if (onClick != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(19.dp), tint = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun DeviceToggleRow(
    @StringRes label: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    @StringRes description: Int? = null,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .toggleable(checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(label),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (description != null) {
                Text(
                    stringResource(description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun stemActionLabel(action: StemAction): String? = when (action) {
    StemAction.CYCLE_NOISE_CONTROL_MODES -> stringResource(R.string.listening_mode)
    StemAction.DIGITAL_ASSISTANT -> stringResource(R.string.digital_assistant)
    else -> null
}
