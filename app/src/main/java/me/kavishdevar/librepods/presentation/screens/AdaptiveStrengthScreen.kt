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

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.presentation.components.AndpopCard
import me.kavishdevar.librepods.presentation.components.AndpopDetailPage
import me.kavishdevar.librepods.presentation.components.AndpopFootnote
import me.kavishdevar.librepods.presentation.components.AndpopSliderRow
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import kotlin.math.roundToInt

@Composable
fun AdaptiveStrengthScreen(viewModel: AirPodsViewModel) {
    val state by viewModel.uiState.collectAsState()
    val id = AACPManager.Companion.ControlCommandIdentifiers.AUTO_ANC_STRENGTH
    // The AirPods store how much noise Adaptive removes; the slider shows how much it lets through.
    val stored = state.controlStates[id]?.getOrNull(0)?.toInt() ?: 50
    var value by remember(stored) { mutableFloatStateOf((100 - stored).toFloat()) }

    AndpopDetailPage(
        title = stringResource(R.string.adaptive_audio),
        subtitle = stringResource(R.string.andpop_adaptive_subtitle),
    ) {
        AndpopCard(Modifier.padding(top = 16.dp)) {
            AndpopSliderRow(
                title = stringResource(R.string.andpop_adaptive_strength),
                valueText = stringResource(R.string.andpop_percent_value, value.roundToInt()),
                value = value,
                onValueChange = { value = it },
                onValueChangeFinished = {
                    viewModel.setControlCommandValue(id, byteArrayOf((100 - value.roundToInt()).toByte()))
                },
                valueRange = 0f..100f,
                steps = 1,
                startLabel = stringResource(R.string.andpop_adaptive_less),
                endLabel = stringResource(R.string.andpop_adaptive_more),
            )
        }
        AndpopFootnote(stringResource(R.string.adaptive_audio_description))
    }
}
