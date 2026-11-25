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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.kavishdevar.librepods.BuildConfig
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.AndpopCard
import me.kavishdevar.librepods.presentation.components.AndpopDetailPage
import me.kavishdevar.librepods.presentation.components.AndpopGroup
import me.kavishdevar.librepods.presentation.components.AndpopPrimaryButton
import me.kavishdevar.librepods.presentation.components.AndpopRow
import me.kavishdevar.librepods.presentation.components.AndpopSectionHeader
import me.kavishdevar.librepods.presentation.components.PillTone
import me.kavishdevar.librepods.presentation.components.StatusPill
import me.kavishdevar.librepods.presentation.theme.AndpopIcons
import me.kavishdevar.librepods.presentation.theme.AndpopType
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.services.ServiceManager
import me.kavishdevar.librepods.utils.LogRecorder
import me.kavishdevar.librepods.utils.XposedState
import java.io.File

private enum class RecorderState { Idle, Recording, Saved }

@Composable
fun TroubleshootingScreen() {
    val context = LocalContext.current
    val palette = LocalAndpopPalette.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val recorder = remember { LogRecorder(context.applicationContext) }
    val logs = remember { mutableStateListOf<File>() }
    var recorderState by remember { mutableStateOf(RecorderState.Idle) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    val savedText = stringResource(R.string.andpop_log_saved_toast)
    val emptyText = stringResource(R.string.andpop_log_empty_toast)
    val deletedText = stringResource(R.string.andpop_log_deleted)
    val undoText = stringResource(R.string.andpop_undo)

    suspend fun reload() {
        val found = withContext(Dispatchers.IO) { LogRecorder.savedLogs(context) }
        logs.clear()
        logs.addAll(found)
    }

    LaunchedEffect(Unit) { reload() }
    LaunchedEffect(recorderState) {
        while (recorderState == RecorderState.Recording) {
            elapsed = System.currentTimeMillis() - startedAt
            delay(500)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            if (recorder.isRecording) scope.launch { recorder.stopAndSave(null) }
        }
    }

    fun deleteWithUndo(file: File) {
        val bytes = runCatching { file.readBytes() }.getOrNull()
        file.delete()
        logs.remove(file)
        scope.launch {
            val result = snackbar.showSnackbar(deletedText, actionLabel = undoText, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed && bytes != null) {
                withContext(Dispatchers.IO) { file.writeBytes(bytes) }
                reload()
            }
        }
    }

    Box {
        AndpopDetailPage(
            title = stringResource(R.string.troubleshooting),
            subtitle = stringResource(R.string.andpop_troubleshoot_subtitle),
        ) {
            AndpopSectionHeader(stringResource(R.string.andpop_record_log))
            val steps = listOf(
                R.string.andpop_log_step1_title to R.string.andpop_log_step1_body,
                R.string.andpop_log_step2_title to R.string.andpop_log_step2_body,
                R.string.andpop_log_step3_title to R.string.andpop_log_step3_body,
            )
            AndpopCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    steps.forEachIndexed { index, (title, body) ->
                        val done = when (recorderState) {
                            RecorderState.Idle -> false
                            RecorderState.Recording -> index == 0
                            RecorderState.Saved -> true
                        }
                        val current = when (recorderState) {
                            RecorderState.Idle -> index == 0
                            RecorderState.Recording -> index == 1
                            RecorderState.Saved -> false
                        }
                        RecorderStep(index + 1, title, body, done, current)
                    }
                }
            }
            if (recorderState == RecorderState.Recording) {
                AndpopPrimaryButton(
                    stringResource(R.string.andpop_log_stop),
                    icon = AndpopIcons.Stop,
                    modifier = Modifier.padding(top = 14.dp),
                    onClick = {
                        scope.launch {
                            val file = recorder.stopAndSave(ServiceManager.getService()?.airpodsInstance?.model?.displayName)
                            recorderState = if (file != null) RecorderState.Saved else RecorderState.Idle
                            Toast.makeText(context, if (file != null) savedText else emptyText, Toast.LENGTH_SHORT).show()
                            reload()
                        }
                    },
                )
                Box(Modifier.fillMaxWidth().padding(top = 10.dp).semantics { liveRegion = LiveRegionMode.Polite }, contentAlignment = Alignment.Center) {
                    StatusPill(stringResource(R.string.andpop_log_recording, formatElapsed(elapsed)), PillTone.Error, pulsing = true)
                }
            } else {
                AndpopPrimaryButton(
                    stringResource(R.string.andpop_log_start),
                    icon = AndpopIcons.Play,
                    modifier = Modifier.padding(top = 14.dp),
                    onClick = {
                        recorder.start(scope)
                        if (recorder.isRecording) {
                            startedAt = System.currentTimeMillis()
                            elapsed = 0
                            recorderState = RecorderState.Recording
                        }
                    },
                )
            }

            AndpopSectionHeader(stringResource(R.string.andpop_saved_logs)) {
                if (logs.isNotEmpty()) {
                    TextButton(onClick = { confirmDeleteAll = true }) {
                        Text(stringResource(R.string.andpop_delete_all), color = palette.pri, style = AndpopType.label)
                    }
                }
            }
            if (logs.isEmpty()) {
                AndpopCard {
                    Text(
                        stringResource(R.string.andpop_no_saved_logs),
                        Modifier.fillMaxWidth().padding(22.dp),
                        color = palette.on2,
                        style = AndpopType.body,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                AndpopGroup {
                    logs.forEach { file ->
                        row { shape ->
                            AndpopRow(
                                shape = shape,
                                title = file.name,
                                supporting = Formatter.formatShortFileSize(context, file.length()),
                                icon = AndpopIcons.Document,
                                showChevron = false,
                                trailing = {
                                    IconButton(onClick = { shareLog(context, file) }) {
                                        Icon(AndpopIcons.Share, stringResource(R.string.andpop_share_log), tint = palette.on2)
                                    }
                                    IconButton(onClick = { deleteWithUndo(file) }) {
                                        Icon(AndpopIcons.Trash, stringResource(R.string.andpop_delete_log), tint = palette.on2)
                                    }
                                },
                            )
                        }
                    }
                }
            }

            AndpopSectionHeader(stringResource(R.string.andpop_common_fixes))
            AndpopGroup {
                listOf(
                    R.string.andpop_fix1_title to R.string.andpop_fix1_body,
                    R.string.andpop_fix2_title to R.string.andpop_fix2_body,
                    R.string.andpop_fix3_title to R.string.andpop_fix3_body,
                ).forEach { (title, body) ->
                    row { shape -> AndpopRow(shape = shape, title = stringResource(title), supporting = stringResource(body)) }
                }
                // Xposed steps only make sense when the framework is installed (FOSS builds only).
                if (!BuildConfig.PLAY_BUILD && XposedState.isAvailable) {
                    row { shape ->
                        AndpopRow(
                            shape = shape,
                            title = stringResource(R.string.andpop_fix_xposed_title),
                            supporting = stringResource(R.string.andpop_fix_xposed_body),
                        )
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            containerColor = palette.s2,
            icon = { Icon(AndpopIcons.Trash, null, tint = palette.error) },
            title = { Text(stringResource(R.string.andpop_delete_all_title), style = AndpopType.title) },
            text = { Text(stringResource(R.string.andpop_delete_all_body), color = palette.on2, style = AndpopType.bodySmall) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteAll = false
                    scope.launch {
                        withContext(Dispatchers.IO) { LogRecorder.savedLogs(context).forEach { it.delete() } }
                        reload()
                    }
                }) { Text(stringResource(R.string.andpop_delete_all), color = palette.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun RecorderStep(number: Int, @StringRes title: Int, @StringRes body: Int, done: Boolean, current: Boolean) {
    val palette = LocalAndpopPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(
                when {
                    done -> palette.success
                    current -> palette.pri
                    else -> palette.s3
                },
            ),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(AndpopIcons.Check, null, Modifier.size(16.dp), tint = palette.bg)
            } else {
                Text(number.toString(), color = if (current) palette.onPri else palette.on2, style = AndpopType.label)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(title), color = palette.on, style = AndpopType.titleSmall)
            Text(stringResource(body), color = palette.on2, style = AndpopType.bodySmall)
        }
    }
}

private fun formatElapsed(ms: Long): String {
    val seconds = ms / 1000
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}

private fun shareLog(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.name)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(send, context.getString(R.string.andpop_share_log)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.andpop_share_unavailable), Toast.LENGTH_SHORT).show()
    }
}
