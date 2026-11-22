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

enum class AndpopTint {
    Glacier, Iris, Guava;

    companion object {
        fun saved(name: String?, dark: Boolean): AndpopTint =
            entries.firstOrNull { it.name == name } ?: if (dark) Glacier else Iris
    }
}

internal class AndpopPalette(val scheme: ColorScheme, val selectedCheck: Color) {
    val connected: Color get() = scheme.onSurfaceVariant
}

private fun palette(dark: Boolean, base: Color, panel: Color, ink: Color, accent: Color): AndpopPalette {
    fun blend(amount: Float) = androidx.compose.ui.graphics.lerp(base, ink, amount)
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        background = base, onBackground = ink, surface = panel, onSurface = ink,
        surfaceContainerLowest = base, surfaceContainerLow = panel,
        surfaceContainer = panel, surfaceContainerHigh = blend(0.12f),
        surfaceContainerHighest = blend(0.17f), surfaceVariant = blend(0.13f),
        onSurfaceVariant = blend(0.78f), primary = accent, onPrimary = base,
        primaryContainer = blend(0.16f), onPrimaryContainer = ink,
        secondary = accent, onSecondary = base, secondaryContainer = blend(0.12f),
        onSecondaryContainer = ink, outline = blend(0.55f), outlineVariant = blend(0.24f),
        surfaceTint = accent, inverseSurface = ink, inverseOnSurface = base,
    )
    return AndpopPalette(scheme, if (dark) Color(0xFF76E6A7) else Color(0xFF147044))
}

internal fun andpopPalette(tint: AndpopTint, dark: Boolean): AndpopPalette = when (tint) {
    AndpopTint.Glacier -> if (dark) palette(true, Color(0xFF0F2932), Color(0xFF28434A), Color(0xFFF2FAFB), Color(0xFFB9E0E9))
        else palette(false, Color(0xFFDEF3F6), Color(0xFFF1FCFD), Color(0xFF122D34), Color(0xFF245460))
    AndpopTint.Iris -> if (dark) palette(true, Color(0xFF211C39), Color(0xFF38324F), Color(0xFFF5F1FF), Color(0xFFE0D2FF))
        else palette(false, Color(0xFFEEE9FF), Color(0xFFF9F6FF), Color(0xFF241E35), Color(0xFF584477))
    AndpopTint.Guava -> if (dark) palette(true, Color(0xFF341E2A), Color(0xFF4E3341), Color(0xFFFFF1F4), Color(0xFFFFD1DD))
        else palette(false, Color(0xFFFFE9E4), Color(0xFFFFF5F1), Color(0xFF352326), Color(0xFF754452))
}

internal val DarkAndpopPalette = andpopPalette(AndpopTint.Glacier, true)
internal val LocalAndpopPalette = staticCompositionLocalOf { DarkAndpopPalette }
internal val LocalAndpopTint = staticCompositionLocalOf { AndpopTint.Glacier }
internal val LocalSetAndpopTint = staticCompositionLocalOf<(AndpopTint) -> Unit> { {} }
val LocalAndpopAppearance = staticCompositionLocalOf { AndpopAppearance.System }
val LocalSetAndpopAppearance = staticCompositionLocalOf<(AndpopAppearance) -> Unit> { {} }

internal fun Modifier.devicePanel(
    palette: AndpopPalette,
    shape: RoundedCornerShape = RoundedCornerShape(23.dp),
    color: Color = palette.scheme.surface,
) = clip(shape)
    .background(Brush.verticalGradient(listOf(androidx.compose.ui.graphics.lerp(color, Color.White, 0.09f), color.copy(alpha = color.alpha * 0.89f))))
    .border(
        1.dp,
        Brush.verticalGradient(
            listOf(androidx.compose.ui.graphics.lerp(palette.scheme.outlineVariant, Color.White, 0.40f), palette.scheme.outlineVariant.copy(alpha = 0.48f)),
        ),
        shape,
    )
