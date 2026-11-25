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

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.AndpopDetailPage
import me.kavishdevar.librepods.presentation.components.AndpopPrimaryButton
import me.kavishdevar.librepods.presentation.components.AndpopTextButton
import me.kavishdevar.librepods.presentation.components.AndpopTextField
import me.kavishdevar.librepods.presentation.theme.AndpopIcons
import me.kavishdevar.librepods.presentation.theme.AndpopType
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel

/** AirPods accept names up to 32 characters. */
internal const val AIRPODS_NAME_MAX = 32

/** True when [draft] can be saved in place of [current]. */
internal fun canSaveAirPodsName(draft: String, current: String): Boolean =
    draft.isNotBlank() && draft.length <= AIRPODS_NAME_MAX && draft != current

@Composable
fun RenameScreen(viewModel: AirPodsViewModel, onSaved: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val palette = LocalAndpopPalette.current
    val current = state.deviceName
    var draft by rememberSaveable { mutableStateOf(current) }
    val focusRequester = remember { FocusRequester() }
    val defaultName = state.modelName.ifBlank { stringResource(R.string.andpop_airpods_fallback) }
    val savedText = stringResource(R.string.andpop_rename_saved)
    val canSave = canSaveAirPodsName(draft, current)

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    fun save() {
        if (!canSaveAirPodsName(draft, current)) return
        viewModel.setName(draft.trim().ifBlank { draft })
        Toast.makeText(context, savedText, Toast.LENGTH_SHORT).show()
        onSaved()
    }

    AndpopDetailPage(
        title = stringResource(R.string.name),
        subtitle = stringResource(R.string.andpop_rename_subtitle),
    ) {
        Spacer(Modifier.height(16.dp))
        AndpopTextField(
            value = draft,
            onValueChange = { draft = it.take(AIRPODS_NAME_MAX) },
            label = stringResource(R.string.andpop_rename_label),
            modifier = Modifier.focusRequester(focusRequester),
            leadingIcon = AndpopIcons.Buds,
            trailing = if (draft.isNotEmpty()) {
                {
                    IconButton(onClick = { draft = "" }) {
                        Icon(AndpopIcons.Close, stringResource(R.string.andpop_rename_clear), tint = palette.on2)
                    }
                }
            } else {
                null
            },
            supporting = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        stringResource(if (draft.isBlank()) R.string.andpop_rename_empty_hint else R.string.andpop_rename_hint),
                        Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = AndpopType.caption,
                    )
                    Text(
                        stringResource(R.string.andpop_rename_counter, draft.length, AIRPODS_NAME_MAX),
                        style = AndpopType.caption,
                    )
                }
            },
            isError = draft.isBlank(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
        )
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AndpopPrimaryButton(stringResource(R.string.andpop_save), onClick = ::save, enabled = canSave)
            if (draft != defaultName) {
                AndpopTextButton(
                    stringResource(R.string.andpop_rename_reset, defaultName),
                    onClick = { draft = defaultName },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
