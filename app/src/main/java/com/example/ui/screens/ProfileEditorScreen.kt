package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.ui.components.ConfirmRemovalDialog
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
import com.example.model.ListeningMode
import com.example.model.PodFormFactor
import com.example.model.PodModel
import com.example.ui.components.DeviceModelIcon
import com.example.ui.components.SignalBarsIcon
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(
    editingProfileId: Long?,
    bondedDevices: List<BondedBluetoothDevice>,
    onLoadProfile: suspend (Long) -> DeviceProfileEntity?,
    onSave: (DeviceProfileEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var existingProfile by remember(editingProfileId) { mutableStateOf<DeviceProfileEntity?>(null) }
    var name by rememberSaveable(editingProfileId) { mutableStateOf("") }
    var selectedModel by rememberSaveable(editingProfileId) { mutableStateOf(PodModel.AIRPODS_PRO_2) }
    var linkedAddress by rememberSaveable(editingProfileId) { mutableStateOf<String?>(null) }
    var linkedName by rememberSaveable(editingProfileId) { mutableStateOf<String?>(null) }
    var minRssi by rememberSaveable(editingProfileId) { mutableFloatStateOf(-85f) }
    var irkHex by rememberSaveable(editingProfileId) { mutableStateOf("") }
    var autoPause by rememberSaveable(editingProfileId) { mutableStateOf(true) }
    var autoResume by rememberSaveable(editingProfileId) { mutableStateOf(false) }
    var onePodMode by rememberSaveable(editingProfileId) { mutableStateOf(false) }
    var draftLoaded by rememberSaveable(editingProfileId) { mutableStateOf(false) }
    var confirmDelete by rememberSaveable(editingProfileId) { mutableStateOf(false) }
    if (confirmDelete && editingProfileId != null) {
        ConfirmRemovalDialog(stringResource(R.string.confirm_delete_title),
            stringResource(R.string.confirm_delete_message, existingProfile?.name.orEmpty()), stringResource(R.string.action_delete),
            onDismiss = { confirmDelete = false }, onConfirm = { confirmDelete = false; onDelete(editingProfileId) })
    }

    LaunchedEffect(editingProfileId) {
        if (editingProfileId != null && editingProfileId > 0L) {
            val loaded = onLoadProfile(editingProfileId)
            if (loaded != null) {
                existingProfile = loaded
                if (draftLoaded) return@LaunchedEffect
                draftLoaded = true
                name = loaded.name
                selectedModel = loaded.model
                linkedAddress = loaded.linkedBluetoothAddress
                linkedName = loaded.linkedBluetoothName
                minRssi = loaded.minRssiDbm.toFloat()
                irkHex = loaded.identityResolvingKeyHex.orEmpty()
                autoPause = loaded.autoPauseEnabled
                autoResume = loaded.autoResumeEnabled
                onePodMode = loaded.onePodModeEnabled
            }
        }
    }

    val cleanedIrk = irkHex.replace(":", "").replace("-", "").replace(" ", "").trim()
    val isIrkValid = cleanedIrk.isEmpty() ||
        (cleanedIrk.length == 32 && cleanedIrk.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' })

    val rssiInt = minRssi.roundToInt()
    val previewBars = when {
        rssiInt >= -55 -> 4
        rssiInt >= -68 -> 3
        rssiInt >= -80 -> 2
        else -> 1
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (editingProfileId != null && editingProfileId > 0L) {
                            stringResource(R.string.title_edit_profile)
                        } else {
                            stringResource(R.string.title_add_profile)
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    if (editingProfileId != null && editingProfileId > 0L) {
                        IconButton(
                            onClick = { confirmDelete = true },
                            modifier = Modifier.testTag("editor_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = stringResource(R.string.cd_delete_profile),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            val finalName = name.trim().ifBlank { selectedModel.displayName }
                            val defaultMode = existingProfile?.preferredListeningModeId
                                ?: if (selectedModel.supportsAdaptive) {
                                    ListeningMode.ADAPTIVE.id
                                } else if (selectedModel.supportsAnc) {
                                    ListeningMode.NOISE_CANCELLATION.id
                                } else {
                                    ListeningMode.OFF.id
                                }
                            val existing = existingProfile
                            onSave(
                                DeviceProfileEntity(
                                    id = existing?.id ?: 0L,
                                    name = finalName,
                                    modelId = selectedModel.id,
                                    linkedBluetoothAddress = linkedAddress,
                                    linkedBluetoothName = linkedName,
                                    minRssiDbm = rssiInt,
                                    identityResolvingKeyHex = cleanedIrk.ifEmpty { null },
                                    autoPauseEnabled = autoPause,
                                    autoResumeEnabled = autoResume,
                                    onePodModeEnabled = onePodMode,
                                    preferredListeningModeId = defaultMode,
                                    conversationalAwarenessEnabled = existing?.conversationalAwarenessEnabled ?: false,
                                    personalizedVolumeEnabled = existing?.personalizedVolumeEnabled ?: false,
                                    allowAncOnePodEnabled = existing?.allowAncOnePodEnabled ?: false,
                                    adaptiveNoiseLevel = existing?.adaptiveNoiseLevel ?: 50,
                                    microphoneModeId = existing?.microphoneModeId ?: com.example.model.MicrophoneMode.AUTOMATIC.id,
                                    lastFirmwareVersion = existing?.lastFirmwareVersion,
                                    lastFirmwareCheckMillis = existing?.lastFirmwareCheckMillis,
                                    createdAtMillis = existing?.createdAtMillis
                                        ?: System.currentTimeMillis()
                                )
                            )
                        },
                        enabled = isIrkValid && (editingProfileId == null || existingProfile != null),
                        modifier = Modifier
                            .widthIn(max = 608.dp)
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_profile_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_save_profile),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
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
                    .widthIn(max = 640.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Name Input
                item(key = "name_field") {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.label_profile_name)) },
                        placeholder = { Text(stringResource(R.string.hint_profile_name)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input")
                    )
                }

                // Model Picker Carousel
                item(key = "model_picker") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.label_device_model),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(
                                items = PodModel.selectableModels,
                                key = { it.id }
                            ) { model ->
                                val isSelected = model == selectedModel
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        }
                                    ),
                                    modifier = Modifier
                                        .width(132.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable {
                                            selectedModel = model
                                            if (name.isBlank()) {
                                                name = model.displayName
                                            }
                                        }
                                        .testTag("model_option_${model.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        DeviceModelIcon(
                                            model = model,
                                            tint = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            ventTint = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainer
                                            },
                                            iconSize = 32.dp
                                        )
                                        Text(
                                            text = model.displayName,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (model.supportsAdaptive || model.supportsAnc) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                                }
                                            ) {
                                                Text(
                                                    text = if (model.supportsAdaptive) {
                                                        stringResource(R.string.label_adaptive_supported)
                                                    } else {
                                                        stringResource(R.string.label_anc_supported)
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Linked Paired Bluetooth Device Selector
                item(key = "linked_bt") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bluetooth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.label_linked_bluetooth),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinkedDeviceRadioRow(
                                title = stringResource(R.string.label_no_linked_device),
                                subtitle = null,
                                selected = linkedAddress == null,
                                onClick = {
                                    linkedAddress = null
                                    linkedName = null
                                }
                            )

                            bondedDevices.forEach { device ->
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                LinkedDeviceRadioRow(
                                    title = device.name,
                                    subtitle = device.address,
                                    selected = linkedAddress.equals(device.address, ignoreCase = true),
                                    onClick = {
                                        linkedAddress = device.address
                                        linkedName = device.name
                                    }
                                )
                            }
                        }
                    }
                }

                // Signal Threshold & Optional IRK
                item(key = "signal_and_irk") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.label_min_rssi),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    SignalBarsIcon(bars = previewBars)
                                    Text(
                                        text = "$rssiInt dBm",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Slider(
                                value = minRssi,
                                onValueChange = { minRssi = it },
                                valueRange = -95f..-45f,
                                modifier = Modifier.testTag("rssi_threshold_slider")
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            OutlinedTextField(
                                value = irkHex,
                                onValueChange = { irkHex = it.uppercase() },
                                label = { Text(stringResource(R.string.label_irk_optional)) },
                                placeholder = { Text(stringResource(R.string.hint_irk)) },
                                supportingText = {
                                    Text(
                                        text = if (isIrkValid) {
                                            stringResource(R.string.helper_irk)
                                        } else {
                                            "Enter exactly 32 hexadecimal characters (16 bytes)."
                                        }
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                isError = !isIrkValid,
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("irk_hex_input")
                            )
                        }
                    }
                }

                // Automatic Ear Detection Controls
                item(key = "ear_detection_card") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            EditorSwitchRow(
                                title = stringResource(R.string.label_auto_pause),
                                subtitle = stringResource(R.string.desc_auto_pause),
                                checked = autoPause,
                                onCheckedChange = { autoPause = it },
                                tag = "auto_pause_switch"
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            EditorSwitchRow(
                                title = stringResource(R.string.label_auto_resume),
                                subtitle = stringResource(R.string.desc_auto_resume),
                                checked = autoResume,
                                onCheckedChange = { autoResume = it },
                                tag = "auto_resume_switch"
                            )
                            if (selectedModel.formFactor == PodFormFactor.DUAL_POD) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                EditorSwitchRow(
                                    title = stringResource(R.string.label_one_pod_mode),
                                    subtitle = stringResource(R.string.desc_one_pod_mode),
                                    checked = onePodMode,
                                    onCheckedChange = { onePodMode = it },
                                    tag = "one_pod_switch"
                                )
                            }
                        }
                    }
                }

                item(key = "bottom_pad") {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun LinkedDeviceRadioRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EditorSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}
