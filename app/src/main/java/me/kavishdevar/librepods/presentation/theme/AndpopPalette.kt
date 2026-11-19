// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.presentation.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R

enum class AndpopAppearance(@param:StringRes val label: Int) {
    System(R.string.andpop_theme_system),
    Light(R.string.andpop_theme_light),
    Dark(R.string.andpop_theme_dark),
}

internal class AndpopPalette(
    val scheme: ColorScheme,
    val connected: Color,
)

internal val DarkAndpopPalette = AndpopPalette(
    scheme = darkColorScheme(
        background = Color(0xFF17171D),
        onBackground = Color(0xFFF4F1F7),
        surface = Color(0xFF23232D),
        surfaceContainerLowest = Color(0xFF14141B),
        surfaceContainerLow = Color(0xFF1C1C26),
        surfaceContainer = Color(0xFF23232D),
        surfaceContainerHigh = Color(0xFF2E2C3B),
        surfaceContainerHighest = Color(0xFF393649),
        surfaceVariant = Color(0xFF393649),
        onSurface = Color(0xFFF4F1F7),
        onSurfaceVariant = Color(0xFFC1BBCD),
        primary = Color(0xFFD9C9FF),
        onPrimary = Color(0xFF2C2043),
        primaryContainer = Color(0xFF4C3B71),
        onPrimaryContainer = Color(0xFFF2E9FF),
        secondary = Color(0xFFD5C5EE),
        secondaryContainer = Color(0xFF35304B),
        onSecondaryContainer = Color(0xFFEEE4FF),
        outline = Color(0xFF9E96AB),
        outlineVariant = Color(0xFF3E394D),
        error = Color(0xFFFFB5AD),
    ),
    connected = Color(0xFF8EDDB3),
)

internal val LightAndpopPalette = AndpopPalette(
    scheme = lightColorScheme(
        background = Color(0xFFF3F0F6),
        onBackground = Color(0xFF211C2A),
        surface = Color(0xFFFFFCFF),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFFAF7FC),
        surfaceContainer = Color(0xFFF3F0F6),
        surfaceContainerHigh = Color(0xFFEDE7F3),
        surfaceContainerHighest = Color(0xFFE6DFEE),
        surfaceVariant = Color(0xFFE6DFEE),
        onSurface = Color(0xFF211C2A),
        onSurfaceVariant = Color(0xFF5F556B),
        primary = Color(0xFF624893),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFECDEFF),
        onPrimaryContainer = Color(0xFF412864),
        secondary = Color(0xFF6B507D),
        secondaryContainer = Color(0xFFEBE1F0),
        onSecondaryContainer = Color(0xFF493553),
        outline = Color(0xFF7E718A),
        outlineVariant = Color(0xFFE1DAE9),
        error = Color(0xFFA63A3A),
    ),
    connected = Color(0xFF1B6E4A),
)

internal val LocalAndpopPalette = staticCompositionLocalOf { DarkAndpopPalette }
val LocalAndpopAppearance = staticCompositionLocalOf { AndpopAppearance.System }
val LocalSetAndpopAppearance = staticCompositionLocalOf<(AndpopAppearance) -> Unit> { {} }

internal fun Modifier.devicePanel(
    palette: AndpopPalette,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    color: Color = palette.scheme.surface,
) = clip(shape)
    .background(Brush.verticalGradient(listOf(color, color.copy(alpha = color.alpha * 0.84f))))
    .border(
        1.dp,
        Brush.verticalGradient(
            listOf(palette.scheme.outlineVariant, palette.scheme.outlineVariant.copy(alpha = 0.48f)),
        ),
        shape,
    )
