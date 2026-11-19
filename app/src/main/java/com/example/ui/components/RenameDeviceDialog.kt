package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DeviceNameRepository

/**
 * Material 3 dialog allowing the user to rename a connected Bluetooth device or profile
 * and persist the custom name in Preferences DataStore.
 */
@Composable
fun RenameDeviceDialog(
    currentName: String,
    defaultName: String,
    bluetoothAddress: String?,
    hasCustomName: Boolean,
    onDismiss: () -> Unit,
    onConfirmRename: (String) -> Unit,
    onResetToDefault: () -> Unit,
) {
    var draftName by rememberSaveable(currentName) { mutableStateOf(currentName) }
    val trimmed = draftName.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("rename_device_dialog"),
        icon = {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(text = stringResource(R.string.rename_dialog_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val subtitle = if (!bluetoothAddress.isNullOrBlank()) {
                    "$defaultName · $bluetoothAddress"
                } else {
                    defaultName
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = draftName,
                    onValueChange = { updated ->
                        if (updated.length <= DeviceNameRepository.MAX_NAME_LENGTH) {
                            draftName = updated
                        }
                    },
                    label = { Text(stringResource(R.string.rename_input_label)) },
                    placeholder = { Text(stringResource(R.string.rename_input_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = if (draftName.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = { draftName = "" },
                                modifier = Modifier.testTag("rename_device_clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.action_clear),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        null
                    },
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.rename_helper_datastore),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${draftName.length}/${DeviceNameRepository.MAX_NAME_LENGTH}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_device_input")
                )

                if (hasCustomName) {
                    TextButton(
                        onClick = {
                            onResetToDefault()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("rename_device_reset_button")
                    ) {
                        Text(stringResource(R.string.action_reset_name))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (trimmed.isNotEmpty()) {
                        onConfirmRename(trimmed)
                        onDismiss()
                    }
                },
                enabled = trimmed.isNotEmpty(),
                modifier = Modifier.testTag("rename_device_save_button")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("rename_device_cancel_button")
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
