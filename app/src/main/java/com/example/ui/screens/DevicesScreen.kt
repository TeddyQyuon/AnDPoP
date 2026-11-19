package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DeviceProfileEntity
import com.example.model.BondedBluetoothDevice
import com.example.ui.components.DeviceModelIcon
import com.example.ui.components.RenameDeviceDialog
import com.example.viewmodel.CAPodUiState
import com.example.viewmodel.MatchedDeviceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    uiState: CAPodUiState,
    onNavigateBack: () -> Unit,
    onOpenDiscovery: () -> Unit,
    onAddProfile: () -> Unit,
    onEditProfile: (Long) -> Unit,
    onDeleteProfile: (Long) -> Unit,
    onRenameDevice: (DeviceProfileEntity, String) -> Unit,
    onResetDeviceName: (DeviceProfileEntity) -> Unit,
    onRenameBondedDevice: (String, String, String) -> Unit,
    onResetBondedDeviceName: (String, String) -> Unit,
    onQuickAddBonded: (BondedBluetoothDevice) -> Unit,
) {
    var pendingDeleteProfile by remember { mutableStateOf<DeviceProfileEntity?>(null) }
    var renamingMatched by remember { mutableStateOf<MatchedDeviceState?>(null) }
    var renamingBonded by remember { mutableStateOf<BondedBluetoothDevice?>(null) }

    val linkedAddresses = remember(uiState.matchedDevices) {
        uiState.matchedDevices.mapNotNull { it.profile.linkedBluetoothAddress?.uppercase() }.toSet()
    }
    val unlinkedBonded = remember(uiState.bondedDevices, linkedAddresses) {
        uiState.bondedDevices.filter { it.address.uppercase() !in linkedAddresses }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_devices),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("devices_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenDiscovery,
                        modifier = Modifier.testTag("devices_discover_ble_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Radar,
                            contentDescription = stringResource(R.string.cd_discover_ble),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProfile,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.cd_add_profile)
                    )
                },
                text = { Text(stringResource(R.string.action_create_profile)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("devices_fab_add_profile")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "saved_header") {
                    Text(
                        text = stringResource(R.string.section_saved_profiles),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                if (uiState.matchedDevices.isEmpty()) {
                    item(key = "empty_profiles") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.devices_empty_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.devices_empty_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = uiState.matchedDevices,
                        key = { "saved_${it.profile.id}" }
                    ) { matched ->
                        val profile = matched.profile
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("saved_profile_card_${profile.id}"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        DeviceModelIcon(
                                            model = profile.model,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            ventTint = MaterialTheme.colorScheme.surfaceContainer,
                                            iconSize = 28.dp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = matched.displayName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = profile.model.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Min ${profile.minRssiDbm} dBm",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (!profile.linkedBluetoothAddress.isNullOrBlank()) {
                                            Icon(
                                                imageVector = Icons.Rounded.Bluetooth,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        if (!profile.identityResolvingKeyHex.isNullOrBlank()) {
                                            Icon(
                                                imageVector = Icons.Rounded.Lock,
                                                contentDescription = stringResource(R.string.cd_encrypted),
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { renamingMatched = matched },
                                    modifier = Modifier.testTag("devices_rename_${profile.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = stringResource(R.string.cd_rename_device),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { onEditProfile(profile.id) },
                                    modifier = Modifier.testTag("devices_edit_${profile.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = stringResource(R.string.cd_edit_profile),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { pendingDeleteProfile = profile },
                                    modifier = Modifier.testTag("devices_delete_${profile.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = stringResource(R.string.cd_delete_profile),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                if (unlinkedBonded.isNotEmpty()) {
                    item(key = "bonded_header") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.section_paired_bluetooth),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    items(
                        items = unlinkedBonded,
                        key = { "bonded_${it.address}" }
                    ) { device ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (device.isConnected) {
                                            Icons.Rounded.BluetoothConnected
                                        } else {
                                            Icons.Rounded.Bluetooth
                                        },
                                        contentDescription = null,
                                        tint = if (device.isConnected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = device.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = device.address,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { renamingBonded = device },
                                    modifier = Modifier.testTag("devices_rename_bonded_${device.address}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = stringResource(R.string.cd_rename_device),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { onQuickAddBonded(device) },
                                    modifier = Modifier.testTag("devices_quick_add_${device.address}"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.action_create_profile))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val toDelete = pendingDeleteProfile
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteProfile = null },
            title = { Text(stringResource(R.string.confirm_delete_title)) },
            text = { Text(stringResource(R.string.confirm_delete_message, toDelete.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteProfile(toDelete.id)
                        pendingDeleteProfile = null
                    },
                    modifier = Modifier.testTag("confirm_delete_profile_button")
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteProfile = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    val matchedToRename = renamingMatched
    if (matchedToRename != null) {
        RenameDeviceDialog(
            currentName = matchedToRename.displayName,
            defaultName = matchedToRename.profile.name,
            bluetoothAddress = matchedToRename.profile.linkedBluetoothAddress,
            hasCustomName = matchedToRename.hasCustomName,
            onDismiss = { renamingMatched = null },
            onConfirmRename = { updated ->
                onRenameDevice(matchedToRename.profile, updated)
            },
            onResetToDefault = {
                onResetDeviceName(matchedToRename.profile)
            }
        )
    }

    val bondedToRename = renamingBonded
    if (bondedToRename != null) {
        RenameDeviceDialog(
            currentName = bondedToRename.displayName,
            defaultName = bondedToRename.name,
            bluetoothAddress = bondedToRename.address,
            hasCustomName = !bondedToRename.customName.isNullOrBlank(),
            onDismiss = { renamingBonded = null },
            onConfirmRename = { updated ->
                onRenameBondedDevice(bondedToRename.address, bondedToRename.name, updated)
            },
            onResetToDefault = {
                onResetBondedDeviceName(bondedToRename.address, bondedToRename.name)
            }
        )
    }
}
