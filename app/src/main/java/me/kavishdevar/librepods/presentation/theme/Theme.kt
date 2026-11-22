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

package me.kavishdevar.librepods.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

val ColorScheme.sectionHeader: Color
    get() = onBackground.copy(alpha = 0.6f)

private val AppleDarkColorScheme = darkColorScheme(
    surfaceContainer = Color(0xFF000000), // for some reason background is not used as the background in gmail and settings app, but surfacecontainer, so using that
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceDim = Color(0x40888888),
    primary = Color(0xFF0091FF),
    secondaryContainer = Color(0xFF366AA8),
    onSecondaryContainer = Color(0xFF0091FF),
    onPrimary = Color(0xFFFFFFFF)
)

private val AppleLightColorScheme = lightColorScheme(
    surfaceContainer = Color(0xFFF2F2F7),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceDim = Color(0x40D9D9D9),
    secondaryContainer = Color(0xFF6BC0FF),
    onSecondaryContainer = Color(0xFF0088FF),
    primary = Color(0xFF0088FF),
    onPrimary = Color(0xFFFFFFFF)
)

@Composable
fun LibrePodsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    m3eEnabled: Boolean = false,
    tint: AndpopTint = if (darkTheme) AndpopTint.Glacier else AndpopTint.Iris,
    content: @Composable () -> Unit
) {
    val palette = androidx.compose.runtime.remember(tint, darkTheme) { andpopPalette(tint, darkTheme) }
    val colorScheme = when {
        m3eEnabled -> palette.scheme
        darkTheme -> AppleDarkColorScheme
        else -> AppleLightColorScheme
    }

    CompositionLocalProvider(
        LocalDesignSystem provides
            if (m3eEnabled) DesignSystem.Material
            else DesignSystem.Apple,
        LocalAndpopPalette provides palette,
        LocalAndpopTint provides tint,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = if (m3eEnabled) MaterialTypography else AppleTypography,
            content = content
        )
    }
}
