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

import android.content.Context
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import me.kavishdevar.librepods.BuildConfig
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.PrivacyLinks
import me.kavishdevar.librepods.presentation.components.AndpopCard
import me.kavishdevar.librepods.presentation.components.AndpopGroup
import me.kavishdevar.librepods.presentation.components.AndpopPageHeader
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
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppSettingsScreen(
    viewModel: AppSettingsViewModel = viewModel(),
    navigateToTroubleshooting: () -> Unit,
    navigateToOpenSourceLicenses: () -> Unit,
    navigateToDeveloperOptions: () -> Unit = {},
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val palette = LocalAndpopPalette.current
    val state by viewModel.uiState.collectAsState()
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var devOptionsEnabled by remember {
        mutableStateOf(!BuildConfig.PLAY_BUILD && prefs.getBoolean("developer_options_enabled", false))
    }
    var devTapCount by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        AndpopPageHeader(stringResource(R.string.settings))

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
                        icon = AndpopIcons.Widget,
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
                    )
                }
                row { shape ->
                    AndpopSwitchRow(
                        shape, stringResource(R.string.relative_conversation_awareness_volume),
                        checked = state.relativeConversationAwarenessVolumeEnabled,
                        onCheckedChange = viewModel::setRelativeConversationAwarenessVolumeEnabled,
                        supporting = stringResource(R.string.relative_conversation_awareness_volume_description),
                    )
                }
                row { shape ->
                    VolumeSliderRow(
                        shape = shape,
                        label = stringResource(R.string.conversation_awareness_volume),
                        value = volume,
                        enabled = true,
                        onValueChange = viewModel::setConversationAwarenessVolume,
                    )
                }
            }

            if (!BuildConfig.PLAY_BUILD &&
                context.checkSelfPermission("android.permission.BLUETOOTH_PRIVILEGED") == PackageManager.PERMISSION_GRANTED
            ) {
                AndpopSectionHeader(stringResource(R.string.ear_detection))
                AndpopGroup {
                    row { shape ->
                        AndpopSwitchRow(
                            shape, stringResource(R.string.disconnect_when_not_wearing),
                            checked = state.disconnectWhenNotWearing,
                            onCheckedChange = viewModel::setDisconnectWhenNotWearing,
                            supporting = stringResource(R.string.disconnect_when_not_wearing_description),
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
                        ConnectChip(stringResource(R.string.takeover_disconnected), state.takeoverWhenDisconnected, true, viewModel::setTakeoverWhenDisconnected)
                        ConnectChip(stringResource(R.string.takeover_idle), state.takeoverWhenIdle, true, viewModel::setTakeoverWhenIdle)
                        ConnectChip(stringResource(R.string.takeover_music), state.takeoverWhenMusic, true, viewModel::setTakeoverWhenMusic)
                        ConnectChip(stringResource(R.string.takeover_call), state.takeoverWhenCall, true, viewModel::setTakeoverWhenCall)
                    }
                    Box(Modifier.padding(vertical = 14.dp).fillMaxWidth().height(1.dp).background(palette.on.copy(alpha = 0.08f)))
                    Text(stringResource(R.string.andpop_connect_when_phone), color = palette.on, style = AndpopType.bodySmall)
                    FlowRow(
                        Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ConnectChip(stringResource(R.string.takeover_ringing_call), state.takeoverWhenRingingCall, true, viewModel::setTakeoverWhenRingingCall)
                        ConnectChip(stringResource(R.string.takeover_media_start), state.takeoverWhenMediaStart, true, viewModel::setTakeoverWhenMediaStart)
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
                    )
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
            row { shape ->
                AndpopRow(shape, stringResource(R.string.troubleshooting), icon = AndpopIcons.Wrench, onClick = navigateToTroubleshooting)
            }
            row { shape ->
                AndpopRow(shape, stringResource(R.string.andpop_privacy), icon = AndpopIcons.Lock, onClick = { showPrivacy = true })
            }
            row { shape ->
                AndpopRow(shape, stringResource(R.string.andpop_credits), icon = AndpopIcons.Heart, onClick = { showCredits = true })
            }
            row { shape ->
                AndpopRow(
                    shape = shape,
                    title = stringResource(R.string.andpop_app_version),
                    value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    icon = AndpopIcons.Info,
                    showChevron = false,
                    onClick = if (!BuildConfig.PLAY_BUILD) {
                        {
                            if (devOptionsEnabled) {
                                Toast.makeText(context, resources.getString(R.string.andpop_developer_already_toast), Toast.LENGTH_SHORT).show()
                            } else {
                                devTapCount++
                                if (devTapCount >= 7) {
                                    devTapCount = 0
                                    devOptionsEnabled = true
                                    prefs.edit { putBoolean("developer_options_enabled", true) }
                                    Toast.makeText(context, resources.getString(R.string.andpop_developer_enabled_toast), Toast.LENGTH_SHORT).show()
                                } else if (devTapCount >= 3) {
                                    Toast.makeText(
                                        context,
                                        resources.getString(R.string.andpop_developer_countdown_toast, 7 - devTapCount),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                        }
                    } else {
                        null
                    },
                )
            }
            if (devOptionsEnabled && !BuildConfig.PLAY_BUILD) {
                row { shape ->
                    AndpopRow(
                        shape = shape,
                        title = stringResource(R.string.andpop_developer_options),
                        icon = AndpopIcons.Code,
                        onClick = navigateToDeveloperOptions,
                    )
                }
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
                        TextButton(onClick = { PrivacyLinks.openPrivacyPolicy(context) }) {
                            Text(stringResource(R.string.andpop_privacy_policy_link), color = palette.pri, style = AndpopType.label)
                            Spacer(Modifier.size(6.dp))
                            Icon(AndpopIcons.OpenExternal, null, Modifier.size(16.dp), tint = palette.pri)
                        }
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
                        StyledListItem(
                            name = stringResource(R.string.andpop_credits_roboto_flex),
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, "https://openfontlicense.org/".toUri()))
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
    }
}

@Composable
private fun PrivacyPoint(title: Int, body: Int) {
    val palette = LocalAndpopPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(title), color = palette.on, style = AndpopType.titleSmall)
        Text(stringResource(body), color = palette.on2, style = AndpopType.supporting)
    }
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = palette.on, style = AndpopType.body)
            Text(stringResource(R.string.andpop_percent_value, value.roundToInt()), color = palette.pri, style = AndpopType.section)
        }
        Slider(
            value = value.coerceIn(10f, 85f),
            onValueChange = { raw ->
                val snapped = if (abs(raw - 44f) <= 2.5f) 44f else raw
                onValueChange(snapped)
            },
            valueRange = 10f..85f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = palette.pri,
                activeTrackColor = palette.pri,
                inactiveTrackColor = palette.s3,
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.andpop_percent_value, 10), color = palette.on3, style = AndpopType.caption)
            Text(stringResource(R.string.andpop_percent_value, 85), color = palette.on3, style = AndpopType.caption)
        }
    }
}

@Composable
private fun ConnectChip(label: String, selected: Boolean, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    val palette = LocalAndpopPalette.current
    FilterChip(
        selected = selected,
        onClick = { onToggle(!selected) },
        enabled = enabled,
        label = { Text(label, style = AndpopType.label) },
        leadingIcon = if (selected) ({ Icon(AndpopIcons.Check, null, Modifier.size(16.dp)) }) else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = palette.pc,
            selectedLabelColor = palette.onPc,
            selectedLeadingIconColor = palette.onPc,
            labelColor = palette.on2,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = palette.line2,
            selectedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
private fun AppearancePicker() {
    val palette = LocalAndpopPalette.current
    val tint = LocalAndpopTint.current
    val current = LocalAndpopAppearance.current
    val setAppearance = LocalSetAndpopAppearance.current
    val context = LocalContext.current

    fun tonesFor(dark: Boolean): Tones = if (tint == AndpopTint.Wallpaper) {
        val dynamic = if (dark) {
            androidx.compose.material3.dynamicDarkColorScheme(context)
        } else {
            androidx.compose.material3.dynamicLightColorScheme(context)
        }
        dynamicTones(dynamic)
    } else {
        andpopTones(tint, dark)
    }

    val light = tonesFor(false)
    val dark = tonesFor(true)
    val cardShape = RoundedCornerShape(24.dp)

    Row(
        Modifier.fillMaxWidth().clip(cardShape).background(palette.s1)
            .padding(horizontal = 10.dp, vertical = 12.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AndpopAppearance.entries.forEach { option ->
            val selected = current == option
            val border = if (selected) palette.pri else palette.line2
            val borderWidth = if (selected) 2.5.dp else 1.dp
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                    .selectable(selected, role = Role.RadioButton) { setAppearance(option) }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier.widthIn(max = 84.dp).fillMaxWidth().aspectRatio(3f / 4.2f)
                        .clip(RoundedCornerShape(14.dp)).border(borderWidth, border, RoundedCornerShape(14.dp)),
                ) {
                    when (option) {
                        AndpopAppearance.System -> Row(Modifier.fillMaxSize()) {
                            MiniDashboard(light, Modifier.weight(1f).fillMaxHeight(), split = true)
                            MiniDashboard(dark, Modifier.weight(1f).fillMaxHeight(), split = true)
                        }
                        AndpopAppearance.Light -> MiniDashboard(light, Modifier.fillMaxSize(), split = false)
                        AndpopAppearance.Dark -> MiniDashboard(dark, Modifier.fillMaxSize(), split = false)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier.size(16.dp).border(2.dp, if (selected) palette.pri else palette.on2, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Box(Modifier.size(8.dp).background(palette.pri, CircleShape))
                    }
                    Text(stringResource(option.label), color = palette.on, style = AndpopType.label)
                }
            }
        }
    }
}

@Composable
private fun MiniDashboard(tones: Tones, modifier: Modifier, split: Boolean) {
    Column(
        modifier.background(tones.bg).padding(if (split) 4.dp else 8.dp),
        verticalArrangement = Arrangement.spacedBy(if (split) 4.dp else 6.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().weight(0.32f).clip(RoundedCornerShape(6.dp))
                .background(Brush.verticalGradient(listOf(lerp(tones.s1, tones.pc, 0.55f), tones.s1))),
        )
        Box(Modifier.fillMaxWidth().weight(0.24f).clip(RoundedCornerShape(6.dp)).background(tones.s1))
        Box(Modifier.fillMaxWidth().weight(0.24f).clip(RoundedCornerShape(6.dp)).background(tones.s1))
        Box(
            Modifier.fillMaxWidth(0.55f).height(if (split) 5.dp else 7.dp).align(Alignment.CenterHorizontally)
                .clip(CircleShape).background(tones.pri),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPicker() {
    val palette = LocalAndpopPalette.current
    val current = LocalAndpopTint.current
    val setTint = LocalSetAndpopTint.current
    val context = LocalContext.current
    val dynamicTones = if (palette.dark) {
        dynamicTones(androidx.compose.material3.dynamicDarkColorScheme(context))
    } else {
        dynamicTones(androidx.compose.material3.dynamicLightColorScheme(context))
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(palette.s1)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlowRow(
            Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AndpopTint.entries.forEach { option ->
                val tones = if (option == AndpopTint.Wallpaper) dynamicTones else andpopTones(option, palette.dark)
                val selected = current == option
                val label = stringResource(option.label)
                val ring = if (selected) palette.pri else palette.line2
                Column(
                    Modifier.widthIn(min = 66.dp)
                        .selectable(selected, role = Role.RadioButton) { setTint(option) }
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape)
                            .border(if (selected) 3.dp else 1.dp, ring, CircleShape)
                            .padding(if (selected) 4.dp else 0.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawCircle(tones.pc)
                            drawArc(tones.pri, startAngle = -45f, sweepAngle = 180f, useCenter = true, size = Size(size.width, size.height))
                        }
                        if (selected) {
                            Box(Modifier.size(22.dp).clip(CircleShape).background(palette.s1), contentAlignment = Alignment.Center) {
                                Icon(AndpopIcons.Check, null, Modifier.size(14.dp), tint = palette.pri)
                            }
                        }
                    }
                    Text(label, color = if (selected) palette.on else palette.on2, style = AndpopType.label)
                }
            }
        }
        val hint = if (current == AndpopTint.Wallpaper) R.string.andpop_color_wallpaper_hint else R.string.andpop_color_applies
        Text(
            stringResource(hint),
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            color = palette.on3,
            style = AndpopType.caption,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SwatchPreviewLabel() {
    Text("82%", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
}
