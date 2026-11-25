// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.presentation.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import me.kavishdevar.librepods.BuildConfig
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.account.AccountViewModel
import me.kavishdevar.librepods.presentation.components.AndpopGroup
import me.kavishdevar.librepods.presentation.components.AndpopPageHeader
import me.kavishdevar.librepods.presentation.components.AndpopRow
import me.kavishdevar.librepods.presentation.components.AndpopSwitchRow
import me.kavishdevar.librepods.presentation.components.InfoBanner
import me.kavishdevar.librepods.presentation.theme.AndpopIcons
import me.kavishdevar.librepods.presentation.theme.AndpopType
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import me.kavishdevar.librepods.presentation.viewmodel.AppSettingsViewModel
import me.kavishdevar.librepods.utils.XposedState
import me.kavishdevar.librepods.utils.bypassDeviceCheck
import me.kavishdevar.librepods.utils.removeDeviceCheckBypass

@Composable
internal fun DeveloperOptionsScreen(
    appSettingsViewModel: AppSettingsViewModel,
    airPodsViewModel: AirPodsViewModel,
    accountViewModel: AccountViewModel?,
    onTurnedOff: () -> Unit,
) {
    if (BuildConfig.PLAY_BUILD) return

    val context = LocalContext.current
    val palette = LocalAndpopPalette.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val appSettings by appSettingsViewModel.uiState.collectAsState()
    val airPodsState by airPodsViewModel.uiState.collectAsState()
    val accountState = accountViewModel?.state?.collectAsState()?.value

    var bypassCheck by remember { mutableStateOf(prefs.getBoolean("bypass_device_check.v2", false)) }
    var showVelocityPlot by remember { mutableStateOf(prefs.getBoolean("dev_show_head_velocity_plot", false)) }
    var showAccountServiceDialog by rememberSaveable { mutableStateOf(false) }
    val restartBluetoothText = stringResource(R.string.found_offset_restart_bluetooth)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        AndpopPageHeader(
            title = stringResource(R.string.andpop_developer_options),
            subtitle = stringResource(R.string.andpop_developer_subtitle),
        )
        InfoBanner(
            text = stringResource(R.string.andpop_developer_warning),
            modifier = Modifier.padding(top = 8.dp, bottom = 14.dp),
            icon = AndpopIcons.Warning,
            warning = true,
        )
        AndpopGroup {
            if (accountViewModel != null && accountState != null) {
                row { shape ->
                    AndpopRow(
                        shape = shape,
                        title = stringResource(R.string.andpop_dev_account_service),
                        supporting = stringResource(R.string.andpop_dev_account_service_desc),
                        icon = AndpopIcons.Layers,
                        onClick = { showAccountServiceDialog = true },
                    )
                }
            }
            row { shape ->
                AndpopSwitchRow(
                    shape = shape,
                    title = stringResource(R.string.bypass_compatibility_check),
                    supporting = stringResource(R.string.andpop_dev_bypass_desc),
                    checked = bypassCheck,
                    onCheckedChange = { enabled ->
                        bypassCheck = enabled
                        if (enabled) bypassDeviceCheck(prefs) else removeDeviceCheckBypass(prefs)
                    },
                    icon = AndpopIcons.Smartphone,
                )
            }
            row { shape ->
                AndpopSwitchRow(
                    shape = shape,
                    title = stringResource(R.string.andpop_dev_demo_mode),
                    supporting = stringResource(R.string.andpop_dev_demo_mode_desc),
                    checked = airPodsState.isDemoMode,
                    onCheckedChange = { enabled -> airPodsViewModel.setDemoMode(enabled) },
                    icon = AndpopIcons.Sparkle,
                )
            }
            if (XposedState.isAvailable && XposedState.bluetoothScopeEnabled) {
                row { shape ->
                    AndpopSwitchRow(
                        shape = shape,
                        title = stringResource(R.string.andpop_dev_apple_device),
                        supporting = stringResource(R.string.andpop_dev_apple_device_desc),
                        checked = appSettings.vendorIdHook,
                        onCheckedChange = { enabled ->
                            Toast.makeText(context, restartBluetoothText, Toast.LENGTH_SHORT).show()
                            appSettingsViewModel.setVendorIdHook(enabled)
                        },
                        icon = AndpopIcons.Code,
                    )
                }
            }
            row { shape ->
                AndpopSwitchRow(
                    shape = shape,
                    title = stringResource(R.string.andpop_dev_velocity_plot),
                    supporting = stringResource(R.string.andpop_dev_velocity_plot_desc),
                    checked = showVelocityPlot,
                    onCheckedChange = { enabled ->
                        showVelocityPlot = enabled
                        prefs.edit { putBoolean("dev_show_head_velocity_plot", enabled) }
                    },
                    icon = AndpopIcons.Head,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        TextButton(
            onClick = {
                prefs.edit { putBoolean("developer_options_enabled", false) }
                onTurnedOff()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.andpop_dev_turn_off), color = palette.pri, style = AndpopType.button)
        }
        Spacer(Modifier.height(32.dp))
    }

    if (showAccountServiceDialog && accountViewModel != null && accountState != null) {
        var url by rememberSaveable { mutableStateOf(accountState.projectUrl) }
        var key by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAccountServiceDialog = false },
            containerColor = palette.s2,
            title = { Text(stringResource(R.string.andpop_account_service_title), style = AndpopType.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.andpop_account_service_body), color = palette.on2, style = AndpopType.bodySmall)
                    accountState.message?.let { msg ->
                        InfoBanner(text = msg, warning = true, icon = AndpopIcons.Warning)
                    }
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(stringResource(R.string.andpop_account_supabase_url)) },
                        placeholder = { Text("https://project-ref.supabase.co") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    )
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        label = { Text(stringResource(R.string.andpop_account_publishable_key)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        accountViewModel.connectProject(url, key)
                        key = ""
                        showAccountServiceDialog = false
                    },
                    enabled = !accountState.busy && !accountState.signedIn && url.isNotBlank() && key.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.pri, contentColor = palette.onPri),
                ) {
                    Text(stringResource(R.string.andpop_account_save_connection))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountServiceDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}
