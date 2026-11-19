package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BlePodSnapshot
import com.example.model.ListeningMode
import com.example.model.PodFormFactor
import com.example.ui.theme.BatteryMidAmber
import com.example.ui.theme.CapodMint
import com.example.viewmodel.MatchedDeviceState

/**
 * Primary CAPod Device Card displaying live battery gauges, charging badges, case bar,
 * status telemetry icons, and ANC mode controls for a saved device profile.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MatchedDeviceCard(
    state: MatchedDeviceState,
    isAncQuickTileLinked: Boolean = false,
    onOpenDetail: () -> Unit,
    onRenameDevice: () -> Unit,
    onEditProfile: () -> Unit,
    onQuickToggleAncMode: () -> Unit,
    onSelectListeningMode: (ListeningMode) -> Unit,
    onPinAncQuickTile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = state.profile
    val model = profile.model
    val snapshot = state.liveSnapshot

    val isActiveConnection = state.isLive || state.isBluetoothConnected
    val cardBorderColor = if (isActiveConnection) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        onClick = onOpenDetail,
        modifier = modifier
            .fillMaxWidth()
            .testTag("device_card_${profile.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActiveConnection) 2.dp else 1.dp)
    ) {
        val headerGlow = Brush.verticalGradient(
            colors = listOf(
                if (isActiveConnection) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.55f)
                },
                MaterialTheme.colorScheme.surface.copy(alpha = 0f),
            )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerGlow)
                .padding(16.dp)
        ) {
            // Header row: Squircle emblem + Name + RSSI bars + Status pills + Rename & Tune buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(52.dp)) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isActiveConnection) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isActiveConnection) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            DeviceModelIcon(
                                model = model,
                                tint = if (isActiveConnection) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                                },
                                ventTint = MaterialTheme.colorScheme.surfaceContainer,
                                iconSize = 30.dp
                            )
                        }
                    }

                    // Crisp status dot anchored to the device badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    state.isLive -> CapodMint
                                    state.isBluetoothConnected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline
                                }
                            )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = state.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (snapshot != null) {
                            SignalBarsIcon(
                                bars = snapshot.signalBars,
                                contentDescription = stringResource(R.string.cd_signal_strength)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = model.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (state.isLive) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f)
                            ) {
                                Text(
                                    text = stringResource(R.string.status_live_ble),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (state.isBluetoothConnected) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.BluetoothConnected,
                                        contentDescription = stringResource(R.string.cd_connected),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.status_connected),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        if (snapshot != null && (snapshot.isLeftInEar || snapshot.isRightInEar)) {
                            Icon(
                                imageVector = Icons.Rounded.Hearing,
                                contentDescription = stringResource(R.string.cd_in_ear),
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (!profile.identityResolvingKeyHex.isNullOrBlank()) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = stringResource(R.string.cd_encrypted),
                                tint = if (snapshot?.isIrkVerified == true) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                },
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (state.firmwareEvaluation.isUpdateAvailable) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BatteryMidAmber.copy(alpha = 0.18f),
                                modifier = Modifier.testTag("firmware_update_badge_${profile.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SystemUpdate,
                                        contentDescription = stringResource(R.string.firmware_update_available),
                                        tint = BatteryMidAmber,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.firmware_update_available),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BatteryMidAmber,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Rename button (minimum 48dp touch target)
                IconButton(
                    onClick = onRenameDevice,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("rename_device_button_${profile.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.cd_rename_device),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Trailing circular outlined Tune button (minimum 48dp touch target)
                IconButton(
                    onClick = onEditProfile,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("edit_profile_button_${profile.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = stringResource(R.string.cd_edit_profile),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Battery Well
            if (model.formFactor == PodFormFactor.SINGLE_HEADSET) {
                val battery = state.effectiveHeadsetBattery
                if (battery != null || snapshot != null) {
                    CompactHeadsetBatteryRow(
                        batteryPercent = battery,
                        isCharging = snapshot?.isHeadsetCharging == true,
                        model = model
                    )
                } else {
                    WaitingForSignalWell(isConnectedOverClassic = state.isBluetoothConnected)
                }
            } else {
                if (snapshot != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                CircularPodBatteryGauge(
                                    batteryPercent = snapshot.leftBattery,
                                    isCharging = snapshot.isLeftCharging,
                                    isInEar = snapshot.isLeftInEar,
                                    isRight = false,
                                    model = model,
                                    modifier = Modifier.weight(1f)
                                )
                                CircularPodBatteryGauge(
                                    batteryPercent = snapshot.rightBattery,
                                    isCharging = snapshot.isRightCharging,
                                    isInEar = snapshot.isRightInEar,
                                    isRight = true,
                                    model = model,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            CaseBatteryBar(
                                caseBatteryPercent = snapshot.caseBattery,
                                isCharging = snapshot.isCaseCharging,
                            )
                        }
                    }
                } else {
                    WaitingForSignalWell(isConnectedOverClassic = state.isBluetoothConnected)
                }
            }

            // ANC Quick Settings Tile Toggle & Listening Mode Bar
            if (model.supportsAnc) {
                Spacer(modifier = Modifier.height(14.dp))
                AncQuickSettingsTileControl(
                    profileId = profile.id,
                    selectedMode = profile.preferredListeningMode,
                    supportsAdaptive = model.supportsAdaptive,
                    isUpdating = state.isUpdatingAnc,
                    isQuickTileLinked = isAncQuickTileLinked,
                    onQuickToggleMode = onQuickToggleAncMode,
                    onSelectMode = onSelectListeningMode,
                    onPinQuickSettingsTile = onPinAncQuickTile,
                )
            }
        }
    }
}

@Composable
private fun WaitingForSignalWell(
    isConnectedOverClassic: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isConnectedOverClassic) {
                    Icons.Rounded.BluetoothConnected
                } else {
                    Icons.Rounded.Podcasts
                },
                contentDescription = null,
                tint = if (isConnectedOverClassic) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isConnectedOverClassic) {
                    stringResource(R.string.status_connected)
                } else {
                    stringResource(R.string.status_waiting_signal)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.status_waiting_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * CAPod's iconic "Unmatched devices" card at the bottom of the Overview dashboard.
 */
@Composable
fun UnmatchedDevicesCard(
    unmatchedDevices: List<BlePodSnapshot>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onQuickAddProfile: (BlePodSnapshot) -> Unit,
    onInspectAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = unmatchedDevices.size
    if (count == 0) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("unmatched_devices_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.unmatched_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (count == 1) {
                    stringResource(R.string.unmatched_singular_body)
                } else {
                    stringResource(R.string.unmatched_plural_body, count)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    unmatchedDevices.forEach { snapshot ->
                        UnmatchedInlineRow(
                            snapshot = snapshot,
                            onAddProfile = { onQuickAddProfile(snapshot) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isExpanded) {
                    TextButton(
                        onClick = onInspectAll,
                        modifier = Modifier.testTag("inspect_unmatched_button")
                    ) {
                        Text(text = stringResource(R.string.action_inspect_all))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                TextButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.testTag("toggle_unmatched_button")
                ) {
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Outlined.VisibilityOff
                        } else {
                            Icons.Outlined.Visibility
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isExpanded) {
                            stringResource(R.string.action_hide)
                        } else {
                            stringResource(R.string.action_show)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UnmatchedInlineRow(
    snapshot: BlePodSnapshot,
    onAddProfile: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeviceModelIcon(
            model = snapshot.model,
            tint = MaterialTheme.colorScheme.onSurface,
            ventTint = MaterialTheme.colorScheme.surfaceContainer,
            iconSize = 26.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = snapshot.model.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                SignalBarsIcon(bars = snapshot.signalBars)
            }
            val batterySummary = if (snapshot.model.formFactor == PodFormFactor.SINGLE_HEADSET) {
                "Battery ${snapshot.headsetBattery ?: "--"}% · ${snapshot.rssi} dBm"
            } else {
                val l = snapshot.leftBattery?.let { "L $it%" } ?: "L --"
                val r = snapshot.rightBattery?.let { "R $it%" } ?: "R --"
                val c = snapshot.caseBattery?.let { "C $it%" } ?: "C --"
                "$l · $r · $c · ${snapshot.rssi} dBm"
            }
            Text(
                text = batterySummary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onAddProfile() }
                .testTag("add_unmatched_${snapshot.address}")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.action_create_profile),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.action_create_profile),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Clean iOS/CAPod-inspired quick battery bottom sheet triggered when a matched case lid opens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionPopupSheet(
    deviceState: MatchedDeviceState,
    onDismiss: () -> Unit,
    onEditProfile: () -> Unit,
) {
    val snapshot = deviceState.liveSnapshot ?: return
    val model = deviceState.profile.model

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = deviceState.displayName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = model.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (model.formFactor == PodFormFactor.SINGLE_HEADSET) {
                CompactHeadsetBatteryRow(
                    batteryPercent = snapshot.headsetBattery,
                    isCharging = snapshot.isHeadsetCharging,
                    model = model
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CircularPodBatteryGauge(
                        batteryPercent = snapshot.leftBattery,
                        isCharging = snapshot.isLeftCharging,
                        isInEar = snapshot.isLeftInEar,
                        isRight = false,
                        model = model,
                        ringSize = 84.dp,
                        modifier = Modifier.weight(1f)
                    )
                    CircularPodBatteryGauge(
                        batteryPercent = snapshot.rightBattery,
                        isCharging = snapshot.isRightCharging,
                        isInEar = snapshot.isRightInEar,
                        isRight = true,
                        model = model,
                        ringSize = 84.dp,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                CaseBatteryBar(
                    caseBatteryPercent = snapshot.caseBattery,
                    isCharging = snapshot.isCaseCharging
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onEditProfile()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("popup_details_button")
                ) {
                    Text(stringResource(R.string.popup_open_details))
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("popup_dismiss_button")
                ) {
                    Text(stringResource(R.string.popup_dismiss))
                }
            }
        }
    }
}
