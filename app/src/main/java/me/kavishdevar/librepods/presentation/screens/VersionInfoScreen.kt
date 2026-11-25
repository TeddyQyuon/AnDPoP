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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.AndpopDetailPage
import me.kavishdevar.librepods.presentation.components.AndpopGroup
import me.kavishdevar.librepods.presentation.components.AndpopRow
import me.kavishdevar.librepods.presentation.components.InfoBanner
import me.kavishdevar.librepods.presentation.theme.AndpopIcons
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel

@Composable
fun VersionScreen(viewModel: AirPodsViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val palette = LocalAndpopPalette.current
    val versions = listOf(state.version1, state.version2, state.version3)
    val dash = stringResource(R.string.andpop_value_unknown)

    AndpopDetailPage(
        title = stringResource(R.string.andpop_firmware_title),
        subtitle = stringResource(R.string.andpop_firmware_subtitle),
    ) {
        Spacer(Modifier.height(16.dp))
        AndpopGroup {
            versions.forEachIndexed { index, version ->
                row { shape ->
                    val label = stringResource(R.string.andpop_firmware_version_n, index + 1)
                    AndpopRow(
                        shape = shape,
                        title = label,
                        value = version.ifBlank { dash },
                        enabled = version.isNotBlank(),
                        showChevron = false,
                        onClick = { copyToClipboard(context, label, version) },
                        trailing = {
                            Icon(AndpopIcons.Copy, stringResource(R.string.andpop_copy_value, label), Modifier.size(18.dp), tint = palette.on3)
                        },
                    )
                }
            }
        }
        InfoBanner(stringResource(R.string.andpop_firmware_note), Modifier.padding(top = 14.dp))
    }
}

internal fun copyToClipboard(context: Context, label: String, value: String) {
    if (value.isBlank()) return
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    // Android 13 and later (the minimum for AnDPoP) confirms the copy itself.
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
}
