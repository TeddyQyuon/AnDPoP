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
import me.kavishdevar.librepods.presentation.components.AndpopFootnote
import me.kavishdevar.librepods.presentation.components.AndpopGroup
import me.kavishdevar.librepods.presentation.components.AndpopRadioRow
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel

/** Stable keys for [me.kavishdevar.librepods.presentation.navigation.Screen.CallControl]. */
internal const val CALL_ACTION_MUTE = "mute"
internal const val CALL_ACTION_HANG_UP = "hang"

/**
 * Mute and hang up always use different presses. The AirPods store one flag:
 * `00 02` means mute is a double press and hang up a single press; `00 03` is the default.
 */
internal fun callControlsFlipped(config: ByteArray?): Boolean = config?.getOrNull(1) == 0x02.toByte()

@Composable
fun CallControlScreen(viewModel: AirPodsViewModel, action: String, onCallControlValueChanged: (Boolean) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val isMute = action == CALL_ACTION_MUTE
    val flipped = callControlsFlipped(state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.CALL_MANAGEMENT_CONFIG])
    val pressOnce = if (isMute) !flipped else flipped

    AndpopDetailPage(
        title = stringResource(if (isMute) R.string.andpop_mute_unmute else R.string.andpop_hang_up),
        subtitle = stringResource(R.string.andpop_call_subtitle),
    ) {
        Spacer(Modifier.height(16.dp))
        AndpopGroup(Modifier.selectableGroup()) {
            row { shape ->
                AndpopRadioRow(
                    shape = shape,
                    title = stringResource(R.string.andpop_press_once),
                    selected = pressOnce,
                    onClick = { onCallControlValueChanged(!isMute) },
                )
            }
            row { shape ->
                AndpopRadioRow(
                    shape = shape,
                    title = stringResource(R.string.andpop_press_twice),
                    selected = !pressOnce,
                    onClick = { onCallControlValueChanged(isMute) },
                )
            }
        }
        AndpopFootnote(stringResource(R.string.andpop_call_linked_note))
    }
}
