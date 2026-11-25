// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.presentation.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext

internal data class SavedThemeSelection(
    val tint: AndpopTint,
    val dark: Boolean,
)

internal fun readSavedThemeSelection(context: Context): SavedThemeSelection {
    val settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val savedAppearance = settings.getString("andpop_appearance", null)
    val appearance = AndpopAppearance.entries.firstOrNull { it.name == savedAppearance } ?: AndpopAppearance.Dark
    val systemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val dark = when (appearance) {
        AndpopAppearance.System -> systemDark
        AndpopAppearance.Light -> false
        AndpopAppearance.Dark -> true
    }
    val tint = AndpopTint.saved(
        settings.getString(ANDPOP_PALETTE_KEY, null) ?: settings.getString(LEGACY_DARK_TINT_KEY, null),
    )
    return SavedThemeSelection(tint = tint, dark = dark)
}

internal fun resolveSavedAndpopPalette(context: Context): AndpopPalette {
    val selection = readSavedThemeSelection(context)
    return if (selection.tint == AndpopTint.Wallpaper) {
        val dynamic = if (selection.dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        andpopPalette(selection.tint, selection.dark, dynamicTones(dynamic))
    } else {
        andpopPalette(selection.tint, selection.dark)
    }
}

@Composable
internal fun SavedAndpopTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val settings = remember(context) { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val savedAppearance = settings.getString("andpop_appearance", null)
    val appearance = AndpopAppearance.entries.firstOrNull { it.name == savedAppearance } ?: AndpopAppearance.Dark
    val dark = when (appearance) {
        AndpopAppearance.System -> isSystemInDarkTheme()
        AndpopAppearance.Light -> false
        AndpopAppearance.Dark -> true
    }
    val tint = AndpopTint.saved(
        settings.getString(ANDPOP_PALETTE_KEY, null) ?: settings.getString(LEGACY_DARK_TINT_KEY, null),
    )
    LibrePodsTheme(darkTheme = dark, m3eEnabled = true, tint = tint, content = content)
}
