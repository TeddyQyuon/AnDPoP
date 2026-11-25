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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.presentation.components.AndpopDetailPage
import me.kavishdevar.librepods.presentation.components.AndpopGroup
import me.kavishdevar.librepods.presentation.components.AndpopRadioRow
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel

@Composable
fun MicrophoneSettingsRoute(viewModel: AirPodsViewModel) {
    val state by viewModel.uiState.collectAsState()
    val id = AACPManager.Companion.ControlCommandIdentifiers.MIC_MODE
    MicrophoneSettingsScreen(
        selectedMode = state.controlStates[id]?.getOrNull(0)?.toInt() ?: 0,
        onMicrophoneSettingsChanged = { viewModel.setControlCommandInt(id, it) },
    )
}

/** Modes in the order the prototype lists them: automatic, always left, always right. */
private val MIC_MODES = listOf(0, 2, 1)

@Composable
fun MicrophoneSettingsScreen(selectedMode: Int, onMicrophoneSettingsChanged: (Int) -> Unit) {
    AndpopDetailPage(
        title = stringResource(R.string.microphone_mode),
        subtitle = stringResource(R.string.andpop_mic_subtitle),
    ) {
        Spacer(Modifier.height(16.dp))
        AndpopGroup(Modifier.selectableGroup()) {
            MIC_MODES.forEach { mode ->
                row { shape ->
                    AndpopRadioRow(
                        shape = shape,
                        title = stringResource(microphoneModeLabel(mode)),
                        supporting = stringResource(microphoneModeDescription(mode)),
                        selected = selectedMode == mode,
                        onClick = { onMicrophoneSettingsChanged(mode) },
                    )
                }
            }
        }
    }
}

@StringRes
internal fun microphoneModeLabel(mode: Int?): Int = when (mode) {
    1 -> R.string.microphone_always_right
    2 -> R.string.microphone_always_left
    else -> R.string.microphone_automatic
}

@StringRes
private fun microphoneModeDescription(mode: Int): Int = when (mode) {
    1 -> R.string.andpop_mic_right_desc
    2 -> R.string.andpop_mic_left_desc
    else -> R.string.andpop_mic_auto_desc
}
