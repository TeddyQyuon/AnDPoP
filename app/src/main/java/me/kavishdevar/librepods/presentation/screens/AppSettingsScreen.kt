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

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import me.kavishdevar.librepods.BuildConfig
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.AndpopCard
import me.kavishdevar.librepods.presentation.components.AndpopGroup
import me.kavishdevar.librepods.presentation.components.AndpopRow
import me.kavishdevar.librepods.presentation.components.AndpopSectionHeader
import me.kavishdevar.librepods.presentation.components.AndpopSwitchRow
import me.kavishdevar.librepods.presentation.components.DeviceInfoCard
import me.kavishdevar.librepods.presentation.components.InfoBanner
import me.kavishdevar.librepods.presentation.components.StyledListItem
import me.kavishdevar.librepods.presentation.theme.AndpopAppearance
import me.kavishdevar.librepods.presentation.theme.AndpopIcons
import me.kavishdevar.librepods.presentation.theme.AndpopTint
import me.kavishdevar.librepods.presentation.theme.AndpopType
import me.kavishdevar.librepods.presentation.theme.LocalAndpopAppearance
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.presentation.theme.LocalAndpopTint
import me.kavishdevar.librepods.presentation.theme.LocalSetAndpopAppearance
import me.kavishdevar.librepods.presentation.theme.LocalSetAndpopTint
import me.kavishdevar.librepods.presentation.theme.Tones
import me.kavishdevar.librepods.presentation.theme.andpopTones
import me.kavishdevar.librepods.presentation.theme.dynamicTones
import me.kavishdevar.librepods.presentation.viewmodel.AppSettingsViewModel
import me.kavishdevar.librepods.utils.XposedState
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppSettingsScreen(
    viewModel: AppSettingsViewModel = viewModel(),
    navigateToTroubleshooting: () -> Unit,
    navigateToOpenSourceLicenses: () -> Unit,
) {
    val context = LocalContext.current
    val palette = LocalAndpopPalette.current
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Text(
            stringResource(R.string.settings),
            Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
            color = palette.on,
            style = AndpopType.pageTitle,
        )

        AndpopSectionHeader(stringResource(R.string.andpop_appearance))
        AppearancePicker()
        AndpopSectionHeader(stringResource(R.string.andpop_color))
        ColorPicker()

        if (state.connectionSuccessful) {
            AndpopSectionHeader(stringResource(R.string.andpop_widget_and_popups))
            AndpopGroup {
                row { shape ->
                    AndpopSwitchRow(
                        shape, stringResource(R.string.show_phone_battery_in_widget),
                        checked = state.showPhoneBatteryInWidget,
                        onCheckedChange = viewModel::setShowPhoneBatteryInWidget,
                        supporting = stringResource(R.string.show_phone_battery_in_widget_description),
                        icon = AndpopIcons.Widget, enabled = state.isPremium,
                    )
                }
                row { shape ->
                    AndpopSwitchRow(
                        shape, stringResource(R.string.show_bottom_sheet_popup),
                        checked = state.showBottomSheetPopup,
                        onCheckedChange = viewModel::setShowBottomSheetPopup,
                        supporting = stringResource(R.string.show_bottom_sheet_popup_description),
                        icon = AndpopIcons.Popup,
                    )
                }
                row { shape ->
                    AndpopSwitchRow(
                        shape, stringResource(R.string.show_island_popup),
                        checked = state.showIslandPopup,
                        onCheckedChange = viewModel::setShowIslandPopup,
                        supporting = stringResource(R.string.show_island_popup_description),
                        icon = AndpopIcons.Island,
                    )
                }
            }
            if ((state.showBottomSheetPopup || state.showIslandPopup) && !Settings.canDrawOverlays(context)) {
                InfoBanner(
                    stringResource(R.string.andpop_overlay_needed),
                    Modifier.padding(top = 8.dp),
                    icon = AndpopIcons.Warning,
                    warning = true,
                ) {
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()),
                        )
                    }) { Text(stringResource(R.string.andpop_allow), color = palette.pri, style = AndpopType.button) }
                }
            }

            AndpopSectionHeader(stringResource(R.string.conversation_awareness))
            val volume = state.conversationAwarenessVolume
            LaunchedEffect(volume) { viewModel.setConversationAwarenessVolume(volume) }
            AndpopGroup {
                row { shape ->
                    AndpopSwitchRow(
                        shape, stringResource(R.string.conversation_awareness_pause_music),
                        checked = state.conversationAwarenessPauseMusicEnabled,
                        onCheckedChange = viewModel::setConversationAwarenessPauseMusicEnabled,
                        supporting = stringResource(R.string.conversation_awareness_pause_music_description),
                        enabled = state.isPremium,
                    )
                }
                row { shape ->
                    AndpopSwitchRow(
                        shape, stringResource(R.string.relative_conversation_awareness_volume),
                        checked = state.relativeConversationAwarenessVolumeEnabled,
                        onCheckedChange = viewModel::setRelativeConversationAwarenessVolumeEnabled,
                        supporting = stringResource(R.string.relative_conversation_awareness_volume_description),
                        enabled = state.isPremium,
                    )
                }
                row { shape ->
                    VolumeSliderRow(
                        shape = shape,
                        label = stringResource(R.string.conversation_awareness_volume),
                        value = volume,
                        enabled = state.isPremium,
                        onValueChange = viewModel::setConversationAwarenessVolume,
                    )
                }
            }

            if (context.checkSelfPermission("android.permission.BLUETOOTH_PRIVILEGED") == PackageManager.PERMISSION_GRANTED) {
                AndpopSectionHeader(stringResource(R.string.ear_detection))
                AndpopGroup {
                    row { shape ->
                        AndpopSwitchRow(
                            shape, stringResource(R.string.disconnect_when_not_wearing),
                            checked = state.disconnectWhenNotWearing,
                            onCheckedChange = viewModel::setDisconnectWhenNotWearing,
                            supporting = stringResource(R.string.disconnect_when_not_wearing_description),
                            enabled = state.isPremium,
                        )
                    }
                }
            }

            AndpopSectionHeader(stringResource(R.string.andpop_automatic_connection))
            AndpopCard(Modifier.padding(0.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.andpop_connect_when_airpods), color = palette.on, style = AndpopType.bodySmall)
                    FlowRow(
                        Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ConnectChip(stringResource(R.string.takeover_disconnected), state.takeoverWhenDisconnected, state.isPremium, viewModel::setTakeoverWhenDisconnected)
                        ConnectChip(stringResource(R.string.takeover_idle), state.takeoverWhenIdle, state.isPremium, viewModel::setTakeoverWhenIdle)
                        ConnectChip(stringResource(R.string.takeover_music), state.takeoverWhenMusic, state.isPremium, viewModel::setTakeoverWhenMusic)
                        ConnectChip(stringResource(R.string.takeover_call), state.takeoverWhenCall, state.isPremium, viewModel::setTakeoverWhenCall)
                    }
                    Box(Modifier.padding(vertical = 14.dp).fillMaxWidth().height(1.dp).background(palette.on.copy(alpha = 0.08f)))
                    Text(stringResource(R.string.andpop_connect_when_phone), color = palette.on, style = AndpopType.bodySmall)
                    FlowRow(
                        Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ConnectChip(stringResource(R.string.takeover_ringing_call), state.takeoverWhenRingingCall, state.isPremium, viewModel::setTakeoverWhenRingingCall)
                        ConnectChip(stringResource(R.string.takeover_media_start), state.takeoverWhenMediaStart, state.isPremium, viewModel::setTakeoverWhenMediaStart)
                    }
                    Text(
                        stringResource(R.string.andpop_connect_note),
                        Modifier.padding(top = 12.dp),
                        color = palette.on3,
                        style = AndpopType.caption,
                    )
                }
            }

            AndpopSectionHeader(stringResource(R.string.andpop_advanced))
            AndpopGroup {
                row { shape ->
                    AndpopSwitchRow(
                        shape, stringResource(R.string.use_alternate_head_tracking_packets),
                        checked = state.useAlternateHeadTrackingPackets,
                        onCheckedChange = viewModel::setUseAlternateHeadTrackingPackets,
                        supporting = stringResource(R.string.use_alternate_head_tracking_packets_description),
                        enabled = state.isPremium,
                    )
                }
                if (XposedState.isAvailable && XposedState.bluetoothScopeEnabled) {
                    row { shape ->
                        val restartBluetoothText = stringResource(R.string.found_offset_restart_bluetooth)
                        AndpopSwitchRow(
                            shape,
                            stringResource(R.string.act_as_an_apple_device) + " (${stringResource(R.string.requires_xposed)})",
                            checked = state.vendorIdHook,
                            onCheckedChange = { enabled ->
                                Toast.makeText(context, restartBluetoothText, Toast.LENGTH_SHORT).show()
                                viewModel.setVendorIdHook(enabled)
                            },
                            supporting = stringResource(R.string.act_as_an_apple_device_description),
                        )
                    }
                }
            }
        } else {
            Spacer(Modifier.height(16.dp))
            InfoBanner(stringResource(R.string.customizations_unavailable))
        }

        var showCredits by rememberSaveable { mutableStateOf(false) }
        var showPrivacy by rememberSaveable { mutableStateOf(false) }
        AndpopSectionHeader(stringResource(R.string.andpop_help_and_about))
        AndpopGroup {
            if (!BuildConfig.PLAY_BUILD) {
                row { shape ->
                    AndpopRow(shape, stringResource(R.string.troubleshooting), icon = AndpopIcons.Wrench, onClick = navigateToTroubleshooting)
                }
            }
            row { shape ->
                AndpopRow(shape, stringResource(R.string.andpop_privacy), icon = AndpopIcons.Lock, onClick = { showPrivacy = true })
            }
            row { shape ->
                AndpopRow(shape, stringResource(R.string.andpop_credits), icon = AndpopIcons.Heart, onClick = { showCredits = true })
            }
            row { shape ->
                AndpopRow(
                    shape, stringResource(R.string.andpop_app_version),
                    value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", icon = AndpopIcons.Info,
                )
            }
        }

        var phoneInfo by rememberSaveable { mutableStateOf(false) }
        Spacer(Modifier.height(12.dp))
        AndpopGroup {
            row { shape ->
                val turn by animateFloatAsState(if (phoneInfo) 180f else 0f, label = "Phone information chevron")
                AndpopRow(
                    shape, stringResource(R.string.andpop_phone_information),
                    supporting = "${android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${android.os.Build.MODEL} · Android ${android.os.Build.VERSION.RELEASE}",
                    icon = AndpopIcons.Smartphone,
                    onClick = { phoneInfo = !phoneInfo },
                    showChevron = false,
                ) {
                    Icon(AndpopIcons.ChevronDown, null, Modifier.size(20.dp).rotate(turn), tint = palette.on3)
                }
            }
        }
        if (phoneInfo) {
            Spacer(Modifier.height(8.dp))
            DeviceInfoCard()
        }

        Text(
            stringResource(R.string.andpop_trademark),
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 24.dp, bottom = 32.dp),
            color = palette.on3,
            style = AndpopType.caption,
            textAlign = TextAlign.Center,
        )

        if (showPrivacy) {
            AlertDialog(
                onDismissRequest = { showPrivacy = false },
                containerColor = palette.s2,
                title = { Text(stringResource(R.string.andpop_privacy_title), style = AndpopType.title) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        PrivacyPoint(R.string.andpop_privacy_bluetooth_title, R.string.andpop_privacy_bluetooth_body)
                        PrivacyPoint(R.string.andpop_privacy_account_title, R.string.andpop_privacy_account_body)
                        PrivacyPoint(R.string.andpop_privacy_tracking_title, R.string.andpop_privacy_tracking_body)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacy = false }) { Text(stringResource(android.R.string.ok)) }
                },
            )
        }

        if (showCredits) {
            AlertDialog(
                onDismissRequest = { showCredits = false },
                containerColor = palette.s2,
                title = { Text(stringResource(R.string.andpop_credits), style = AndpopType.title) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        StyledListItem(
                            name = stringResource(R.string.andpop_upstream),
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/librepods-org/librepods".toUri()))
                            },
                        )
                        StyledListItem(
                            name = stringResource(R.string.andpop_photo_credit),
                            description = stringResource(R.string.andpop_photo_credit_detail),
                            onClick = {
                                context.startActivity(Intent(
                                    Intent.ACTION_VIEW,
                                    "https://commons.wikimedia.org/wiki/File:AirPods_Pro_(2nd_generation).jpg".toUri(),
                                ))
                            },
                        )
                        StyledListItem(
                            name = stringResource(R.string.andpop_photo_license),
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, "https://creativecommons.org/licenses/by-sa/4.0/".toUri()))
                            },
                        )
                        StyledListItem(
                            name = stringResource(R.string.andpop_max_photo_credit),
                            description = stringResource(R.string.andpop_max_photo_detail),
                            onClick = {
                                context.startActivity(Intent(
                                    Intent.ACTION_VIEW,
                                    "https://commons.wikimedia.org/wiki/File:Apple_airpods_max_1.jpg".toUri(),
                                ))
                            },
                        )
                        StyledListItem(
                            name = stringResource(R.string.andpop_max_photo_license),
                            onClick = {
                                context.startActivity(Intent(
                                    Intent.ACTION_VIEW,
                                    "https://creativecommons.org/licenses/by-sa/3.0/de/deed.en".toUri(),
                                ))
                            },
                        )
                        StyledListItem(name = stringResource(R.string.open_source_licenses), onClick = {
                            showCredits = false
                            navigateToOpenSourceLicenses()
                        })
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCredits = false }) { Text(stringResource(android.R.string.ok)) }
                },
            )
        }

        if (state.showCameraDialog) {
            AlertDialog(onDismissRequest = { viewModel.setShowCameraDialog(false) }, title = {
                Text(
                    stringResource(R.string.set_custom_camera_package),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium
                )
            }, text = {
                Column {
                    Text(
                        stringResource(R.string.enter_custom_camera_package),
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = state.cameraPackageValue,
                        onValueChange = {
                            viewModel.setCameraPackageValue(it)
                            viewModel.setCameraPackageError(null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.cameraPackageError != null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            capitalization = KeyboardCapitalization.None
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        supportingText = {
                            if (state.cameraPackageError != null) {
                                Text(state.cameraPackageError ?: "", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        label = { Text(stringResource(R.string.custom_camera_package)) })
                }
            }, confirmButton = {
                val successText = stringResource(R.string.custom_camera_package_set_success)
                TextButton(onClick = {
                    viewModel.saveCameraPackage()
                    Toast.makeText(context, successText, Toast.LENGTH_SHORT).show()
                }) { Text("Save", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium) }
            }, dismissButton = {
                TextButton(onClick = { viewModel.setShowCameraDialog(false) }) {
                    Text("Cancel", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium)
                }
            })
        }
    }
}

@Composable
private fun PrivacyPoint(title: Int, body: Int) {
    val palette = LocalAndpopPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(title), color = palette.on, style = AndpopType.titleSmall)
        Text(stringResource(body), color = palette.on2, style = AndpopType.bodySmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectChip(label: String, selected: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val palette = LocalAndpopPalette.current
    val check: (@Composable () -> Unit)? = if (selected) {
        { Icon(AndpopIcons.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
    } else {
        null
    }
    FilterChip(
        selected = selected,
        onClick = { onChange(!selected) },
        enabled = enabled,
        label = { Text(label, style = AndpopType.bodySmall) },
        leadingIcon = check,
        shape = RoundedCornerShape(10.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = palette.on2,
            selectedContainerColor = palette.pc,
            selectedLabelColor = palette.onPc,
            selectedLeadingIconColor = palette.onPc,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = palette.on.copy(alpha = 0.15f),
            selectedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
private fun VolumeSliderRow(
    shape: androidx.compose.ui.graphics.Shape,
    label: String,
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
) {
    val palette = LocalAndpopPalette.current
    Column(
        Modifier.fillMaxWidth().clip(shape).background(palette.s1).padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), color = palette.on, style = AndpopType.body)
            Text("${value.roundToInt()}%", color = palette.pri, style = AndpopType.label)
        }
        Slider(
            value = value,
            onValueChange = { raw ->
                // Snap to the default level so it is easy to return to.
                onValueChange(if (abs(raw - 44f) < 1.5f) 44f else raw)
            },
            valueRange = 10f..85f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = palette.pri,
                activeTrackColor = palette.pri,
                inactiveTrackColor = palette.s3,
            ),
        )
        Row(Modifier.fillMaxWidth()) {
            Text("10%", Modifier.weight(1f), color = palette.on3, style = AndpopType.caption)
            Text("85%", color = palette.on3, style = AndpopType.caption)
        }
    }
}

/** Tones for a theme in one appearance. Wallpaper reads the phone's dynamic colors. */
@Composable
private fun themeTones(tint: AndpopTint, dark: Boolean): Tones {
    val context = LocalContext.current
    return if (tint == AndpopTint.Wallpaper) {
        dynamicTones(
            if (dark) androidx.compose.material3.dynamicDarkColorScheme(context)
            else androidx.compose.material3.dynamicLightColorScheme(context),
        )
    } else {
        andpopTones(tint, dark)
    }
}

/** System, Light and Dark as live previews drawn in the current color theme. */
@Composable
private fun AppearancePicker() {
    val palette = LocalAndpopPalette.current
    val appearance = LocalAndpopAppearance.current
    val setAppearance = LocalSetAndpopAppearance.current
    val tint = LocalAndpopTint.current
    val light = themeTones(tint, false)
    val dark = themeTones(tint, true)
    AndpopCard {
        Row(
            Modifier.fillMaxWidth().padding(12.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AndpopAppearance.entries.forEach { option ->
                val selected = appearance == option
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                        .selectable(selected, role = Role.RadioButton, onClick = { setAppearance(option) })
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(Modifier.widthIn(max = 92.dp).fillMaxWidth().aspectRatio(3f / 4f)) {
                        val previewShape = RoundedCornerShape(16.dp)
                        Box(
                            Modifier.fillMaxSize().clip(previewShape)
                                .border(if (selected) 2.5.dp else 1.dp, if (selected) palette.pri else palette.on.copy(alpha = 0.15f), previewShape),
                        ) {
                            when (option) {
                                AndpopAppearance.Light -> ModePreview(light)
                                AndpopAppearance.Dark -> ModePreview(dark)
                                AndpopAppearance.System -> {
                                    ModePreview(light)
                                    Box(Modifier.fillMaxSize().clip(HalfShape)) { ModePreview(dark) }
                                }
                            }
                        }
                        if (selected) {
                            Box(
                                Modifier.align(Alignment.TopEnd).offset(6.dp, (-6).dp).size(22.dp).clip(CircleShape)
                                    .background(palette.pri),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(AndpopIcons.Check, null, Modifier.size(14.dp), tint = palette.onPri)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        val icon = when (option) {
                            AndpopAppearance.System -> AndpopIcons.Auto
                            AndpopAppearance.Light -> AndpopIcons.Sun
                            AndpopAppearance.Dark -> AndpopIcons.Moon
                        }
                        Icon(icon, null, Modifier.size(18.dp), tint = if (selected) palette.pri else palette.on2)
                        Text(stringResource(option.label), color = if (selected) palette.on else palette.on2, style = AndpopType.label)
                    }
                }
            }
        }
    }
}

/** Right half of a box, used for the System preview. */
private val HalfShape = object : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ) = androidx.compose.ui.graphics.Outline.Rectangle(
        androidx.compose.ui.geometry.Rect(size.width / 2f, 0f, size.width, size.height),
    )
}

/** Small mock of the dashboard in one theme: hero card, battery dots, mode track, a row with a switch. */
@Composable
private fun ModePreview(t: Tones) {
    Column(
        Modifier.fillMaxSize().background(t.bg).padding(horizontal = 8.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))
                .background(Brush.verticalGradient(listOf(lerp(t.s1, t.pc, 0.5f), t.s1))).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.fillMaxWidth(0.58f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(t.on))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                repeat(3) { Box(Modifier.size(11.dp).clip(CircleShape).background(t.on2.copy(alpha = 0.55f))) }
            }
        }
        Row(Modifier.fillMaxWidth().height(15.dp).clip(RoundedCornerShape(8.dp)).background(t.s2).padding(2.dp)) {
            Box(Modifier.fillMaxWidth(0.36f).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(t.pc))
        }
        Row(
            Modifier.fillMaxWidth().height(15.dp).clip(RoundedCornerShape(7.dp)).background(t.s1).padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.fillMaxWidth(0.45f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(t.on2.copy(alpha = 0.7f)))
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(14.dp, 8.dp).clip(RoundedCornerShape(4.dp)).background(t.pri))
        }
    }
}

/** Seven color themes as Material You style swatches. */
@Composable
private fun ColorPicker() {
    val palette = LocalAndpopPalette.current
    val tint = LocalAndpopTint.current
    val setTint = LocalSetAndpopTint.current
    AndpopCard {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 14.dp).selectableGroup(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AndpopTint.entries.chunked(4).forEach { rowOptions ->
                Row(Modifier.fillMaxWidth()) {
                    rowOptions.forEach { option ->
                        val selected = tint == option
                        val darkTones = themeTones(option, true)
                        val lightTones = themeTones(option, false)
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                .selectable(selected, role = Role.RadioButton, onClick = { setTint(option) })
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                Modifier.size(64.dp).clip(CircleShape)
                                    .border(2.5.dp, if (selected) palette.pri else Color.Transparent, CircleShape)
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                val top = darkTones.pri
                                val left = lightTones.pri
                                val right = lightTones.pc
                                Canvas(Modifier.fillMaxSize()) {
                                    drawArc(top, 180f, 180f, useCenter = true)
                                    drawArc(left, 90f, 90f, useCenter = true)
                                    drawArc(right, 0f, 90f, useCenter = true)
                                }
                                if (selected) {
                                    Box(Modifier.size(26.dp).clip(CircleShape).background(palette.s1), contentAlignment = Alignment.Center) {
                                        Icon(AndpopIcons.Check, null, Modifier.size(16.dp), tint = palette.pri)
                                    }
                                }
                            }
                            Text(
                                stringResource(option.label),
                                color = if (selected) palette.on else palette.on2,
                                style = AndpopType.label,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    repeat(4 - rowOptions.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val wallpaper = tint == AndpopTint.Wallpaper
                Icon(if (wallpaper) AndpopIcons.Sparkle else AndpopIcons.Info, null, Modifier.size(16.dp), tint = palette.pri)
                Text(
                    stringResource(if (wallpaper) R.string.andpop_color_wallpaper_hint else R.string.andpop_color_applies),
                    color = palette.on2,
                    style = AndpopType.caption,
                )
            }
        }
    }
}
