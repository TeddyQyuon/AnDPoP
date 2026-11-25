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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import me.kavishdevar.librepods.BuildConfig
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.bluetooth.ATTHandles
import me.kavishdevar.librepods.presentation.components.AndpopDetailPage
import me.kavishdevar.librepods.presentation.components.AndpopFootnote
import me.kavishdevar.librepods.presentation.components.AndpopGroup
import me.kavishdevar.librepods.presentation.components.AndpopSectionHeader
import me.kavishdevar.librepods.presentation.components.AndpopSwitchRow
import me.kavishdevar.librepods.presentation.theme.AndpopIcons
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel

@Composable
fun HearingProtectionScreen(viewModel: AirPodsViewModel) {
    val state by viewModel.uiState.collectAsState()
    val ppeId = AACPManager.Companion.ControlCommandIdentifiers.PPE_TOGGLE_CONFIG

    AndpopDetailPage(title = stringResource(R.string.hearing_protection)) {
        // Loud sound reduction is written over ATT, which needs the Xposed vendor hook.
        if (state.vendorIdHook && !BuildConfig.PLAY_BUILD) {
            AndpopSectionHeader(stringResource(R.string.environmental_noise))
            AndpopGroup {
                row { shape ->
                    AndpopSwitchRow(
                        shape = shape,
                        title = stringResource(R.string.loud_sound_reduction),
                        supporting = stringResource(R.string.andpop_hearing_protection_lsr_desc),
                        icon = AndpopIcons.Shield,
                        checked = state.loudSoundReductionEnabled,
                        onCheckedChange = {
                            viewModel.setATTCharacteristicValue(ATTHandles.LOUD_SOUND_REDUCTION, byteArrayOf(if (it) 1 else 0))
                        },
                    )
                }
            }
        }
        AndpopSectionHeader(stringResource(R.string.andpop_hearing_protection_ppe))
        AndpopGroup {
            row { shape ->
                AndpopSwitchRow(
                    shape = shape,
                    title = stringResource(R.string.ppe),
                    supporting = stringResource(R.string.andpop_hearing_protection_ppe_desc),
                    icon = AndpopIcons.Hearing,
                    checked = state.controlStates[ppeId]?.getOrNull(0)?.toInt() == 1,
                    onCheckedChange = { viewModel.setControlCommandBoolean(ppeId, it) },
                )
            }
        }
        AndpopFootnote(stringResource(R.string.workspace_use_description))
    }
}
