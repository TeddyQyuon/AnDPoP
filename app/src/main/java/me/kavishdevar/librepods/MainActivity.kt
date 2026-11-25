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

package me.kavishdevar.librepods

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import me.kavishdevar.librepods.account.AccountViewModel
import me.kavishdevar.librepods.data.ControlCommandRepository
import me.kavishdevar.librepods.presentation.navigation.NavigationRoot
import me.kavishdevar.librepods.presentation.theme.ANDPOP_PALETTE_KEY
import me.kavishdevar.librepods.presentation.theme.AndpopAppearance
import me.kavishdevar.librepods.presentation.theme.AndpopTint
import me.kavishdevar.librepods.presentation.theme.LEGACY_DARK_TINT_KEY
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.theme.LocalAndpopAppearance
import me.kavishdevar.librepods.presentation.theme.LocalSetAndpopAppearance
import me.kavishdevar.librepods.presentation.theme.LocalSetAndpopTint
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import me.kavishdevar.librepods.services.AirPodsService
import me.kavishdevar.librepods.utils.XposedState

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    companion object {
        init {
            if (XposedState.isAvailable && XposedState.bluetoothScopeEnabled) {
                System.loadLibrary("l2c_fcr_hook")
            }
        }
    }

    private val account: AccountViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleAuthIntent(intent)

        val settings = getSharedPreferences("settings", MODE_PRIVATE)

        setContent {
            val airPodsViewModel: AirPodsViewModel = viewModel()
            LaunchedEffect(account) {
                account.browser.collect { uri ->
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
                    } catch (_: ActivityNotFoundException) {
                        account.browserUnavailable()
                    }
                }
            }
            var onboardingComplete by remember { mutableStateOf(settings.getBoolean("onboarding_complete", false)) }
            DisposableEffect(onboardingComplete, airPodsViewModel) {
                if (onboardingComplete) {
                    val serviceIntent = Intent(applicationContext, AirPodsService::class.java)
                    val connection = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                            val service = (binder as AirPodsService.LocalBinder).getService()
                            airPodsViewModel.init(
                                service,
                                ControlCommandRepository(service.aacpManager),
                                settings,
                                applicationContext,
                            )
                        }

                        override fun onServiceDisconnected(name: ComponentName) {
                            airPodsViewModel.detachService()
                        }
                    }

                    startForegroundService(serviceIntent)
                    val bound = applicationContext.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
                    onDispose {
                        if (bound) applicationContext.unbindService(connection)
                    }
                } else {
                    onDispose {}
                }
            }

            fun loadAppearance(): AndpopAppearance {
                val saved = settings.getString("andpop_appearance", null)
                return AndpopAppearance.entries.firstOrNull { it.name == saved } ?: AndpopAppearance.Dark
            }
            var appearance by remember { mutableStateOf(loadAppearance()) }
            fun loadTint() = AndpopTint.saved(
                settings.getString(ANDPOP_PALETTE_KEY, null) ?: settings.getString(LEGACY_DARK_TINT_KEY, null),
            )
            var tint by remember { mutableStateOf(loadTint()) }
            DisposableEffect(settings) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == "andpop_appearance" || key == null) appearance = loadAppearance()
                    if (key == ANDPOP_PALETTE_KEY || key == null) tint = loadTint()
                }
                settings.registerOnSharedPreferenceChangeListener(listener)
                onDispose { settings.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            val dark = when (appearance) {
                AndpopAppearance.System -> isSystemInDarkTheme()
                AndpopAppearance.Light -> false
                AndpopAppearance.Dark -> true
            }
            LaunchedEffect(dark) {
                val transparent = android.graphics.Color.TRANSPARENT
                val bars = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(
                    transparent, android.graphics.Color.argb(120, 17, 21, 29),
                )
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            CompositionLocalProvider(
                LocalSetAndpopTint provides { preference ->
                    settings.edit { putString(ANDPOP_PALETTE_KEY, preference.name) }
                    tint = preference
                },
                LocalAndpopAppearance provides appearance,
                LocalSetAndpopAppearance provides { preference ->
                    settings.edit { putString("andpop_appearance", preference.name) }
                    appearance = preference
                },
            ) {
                LibrePodsTheme(darkTheme = dark, m3eEnabled = true, tint = tint) {
                    NavigationRoot(
                        showOnboarding = !onboardingComplete,
                        onboardingComplete = {
                            settings.edit { putBoolean("onboarding_complete", true) }
                            onboardingComplete = true
                            account.ensureGuestEntered()
                        },
                        airPodsViewModel = airPodsViewModel,
                        accountViewModel = account,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_VIEW) return
        val callback = intent.dataString ?: return
        intent.data = null
        account.callback(callback)
    }
}
