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

import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.data.parseTransparencySettingsResponse
import me.kavishdevar.librepods.data.sendTransparencySettings
import me.kavishdevar.librepods.presentation.components.AndpopDetailPage
import me.kavishdevar.librepods.presentation.components.AndpopFootnote
import me.kavishdevar.librepods.presentation.components.AndpopGroup
import me.kavishdevar.librepods.presentation.components.AndpopRow
import me.kavishdevar.librepods.presentation.components.AndpopSwitchRow
import me.kavishdevar.librepods.presentation.theme.AndpopIcons
import me.kavishdevar.librepods.presentation.theme.AndpopType
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import kotlin.io.encoding.ExperimentalEncodingApi

private const val TAG = "HearingAidScreen"

@ExperimentalHazeMaterialsApi
@OptIn(ExperimentalEncodingApi::class)
@Composable
fun HearingAidScreen(
    viewModel: AirPodsViewModel,
    onNavigateHearingAidAdjustments: () -> Unit,
    onNavigateHearingTest: () -> Unit,
) {
    val palette = LocalAndpopPalette.current
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    val reportedEnabled =
        (state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.HEARING_AID]?.getOrNull(1) == 0x01.toByte()) &&
            (state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.HEARING_ASSIST_CONFIG]?.getOrNull(0) == 0x01.toByte())

    var hearingAidEnabled by rememberSaveable { mutableStateOf(reportedEnabled) }
    var showEnableDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(reportedEnabled) {
        hearingAidEnabled = reportedEnabled
    }

    AndpopDetailPage(
        title = stringResource(R.string.hearing_aid),
        subtitle = stringResource(R.string.hearing_aid_description),
    ) {
        AndpopGroup {
            row { shape ->
                AndpopSwitchRow(
                    shape = shape,
                    title = stringResource(R.string.hearing_aid),
                    checked = hearingAidEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            showEnableDialog = true
                        } else {
                            hearingAidEnabled = false
                            viewModel.setControlCommandValue(
                                AACPManager.Companion.ControlCommandIdentifiers.HEARING_AID,
                                byteArrayOf(0x01, 0x02),
                            )
                            viewModel.setControlCommandByte(
                                AACPManager.Companion.ControlCommandIdentifiers.HEARING_ASSIST_CONFIG,
                                0x02.toByte(),
                            )
                        }
                    },
                    icon = AndpopIcons.Hearing,
                )
            }
            row { shape ->
                AndpopRow(
                    shape = shape,
                    title = stringResource(R.string.adjustments),
                    supporting = stringResource(R.string.andpop_hearing_adjust_desc),
                    icon = AndpopIcons.Tune,
                    onClick = onNavigateHearingAidAdjustments,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        AndpopGroup {
            row { shape ->
                AndpopRow(
                    shape = shape,
                    title = stringResource(R.string.update_hearing_test),
                    supporting = stringResource(R.string.andpop_hearing_test_desc),
                    icon = AndpopIcons.Document,
                    onClick = onNavigateHearingTest,
                )
            }
        }

        AndpopFootnote(stringResource(R.string.feature_availability_disclaimer))
    }

    if (showEnableDialog) {
        AlertDialog(
            onDismissRequest = { showEnableDialog = false },
            containerColor = palette.s2,
            icon = { Icon(AndpopIcons.Hearing, null, tint = palette.pri) },
            title = {
                Text(stringResource(R.string.andpop_hearing_enable_title), color = palette.on, style = AndpopType.title)
            },
            text = {
                Text(stringResource(R.string.andpop_hearing_enable_body), color = palette.on2, style = AndpopType.bodySmall)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEnableDialog = false
                        hearingAidEnabled = true
                        viewModel.setControlCommandValue(
                            AACPManager.Companion.ControlCommandIdentifiers.HEARING_AID,
                            byteArrayOf(0x01, 0x01),
                        )
                        viewModel.setControlCommandByte(
                            AACPManager.Companion.ControlCommandIdentifiers.HEARING_ASSIST_CONFIG,
                            0x01.toByte(),
                        )
                        scope.launch(Dispatchers.IO) {
                            try {
                                val data = state.hearingAidData
                                if (data.isEmpty()) {
                                    Log.w(TAG, "hearingAidData empty when disabling transparency")
                                    return@launch
                                }
                                val parsed = parseTransparencySettingsResponse(data) ?: return@launch
                                sendTransparencySettings(viewModel::setATTCharacteristicValue, parsed.copy(enabled = false))
                            } catch (e: Exception) {
                                Log.e(TAG, "Error disabling transparency: ${e.message}")
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.andpop_enable), color = palette.pri)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEnableDialog = false }) {
                    Text(stringResource(android.R.string.cancel), color = palette.on2)
                }
            },
        )
    }
}
