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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R

enum class AndpopAppearance(@param:StringRes val label: Int) {
    System(R.string.andpop_theme_system),
    Light(R.string.andpop_theme_light),
    Dark(R.string.andpop_theme_dark),
}

/**
 * Color themes. One choice applies to both light and dark. Wallpaper follows the
 * phone's Material You colors; the others come from one shared OKLCH tonal scale,
 * so every theme has the same text contrast.
 */
enum class AndpopTint(@param:StringRes val label: Int) {
    Wallpaper(R.string.andpop_theme_wallpaper),
    Glacier(R.string.andpop_theme_glacier),
    Iris(R.string.andpop_theme_iris),
    Guava(R.string.andpop_theme_guava),
    Sage(R.string.andpop_theme_sage),
    Apricot(R.string.andpop_theme_apricot),
    Graphite(R.string.andpop_theme_graphite);

    companion object {
        val Default = Glacier

        fun saved(name: String?): AndpopTint = entries.firstOrNull { it.name == name } ?: Default
    }
}

/** Preference key for the color theme. Older builds stored one theme per appearance. */
const val ANDPOP_PALETTE_KEY = "andpop_palette"
const val LEGACY_DARK_TINT_KEY = "andpop_dark_tint"

/** Tonal roles shared by every theme (names match the v2 prototype CSS variables). */
internal class Tones(
    bg: Long, s1: Long, s2: Long, s3: Long, s4: Long,
    on: Long, on2: Long, on3: Long,
    pri: Long, onPri: Long, pc: Long, onPc: Long, glow: Long,
) {
    val bg = Color(bg); val s1 = Color(s1); val s2 = Color(s2); val s3 = Color(s3); val s4 = Color(s4)
    val on = Color(on); val on2 = Color(on2); val on3 = Color(on3)
    val pri = Color(pri); val onPri = Color(onPri); val pc = Color(pc); val onPc = Color(onPc); val glow = Color(glow)
}

internal class AndpopPalette(val scheme: ColorScheme, val dark: Boolean, tones: Tones) {
    /** Page background. */
    val bg = tones.bg
    /** Cards and list rows. */
    val s1 = tones.s1
    /** Inset controls such as the listening-mode track. */
    val s2 = tones.s2
    val s3 = tones.s3
    val s4 = tones.s4
    val on = tones.on
    val on2 = tones.on2
    /** Hints and tertiary text. */
    val on3 = tones.on3
    val pri = tones.pri
    val onPri = tones.onPri
    val pc = tones.pc
    val onPc = tones.onPc
    /** Soft radial glow at the top of the dashboard. */
    val glow = tones.glow
    val line = on.copy(alpha = 0.08f).compositeOver(s1)
    val line2 = on.copy(alpha = 0.15f).compositeOver(s1)

    val success = if (dark) Color(0xFF7BE0A0) else Color(0xFF1B7A43)
    val successContainer = success.copy(alpha = if (dark) 0.14f else 0.10f)
    val warning = if (dark) Color(0xFFFFB68A) else Color(0xFFA3470F)
    val error = if (dark) Color(0xFFFFB4AB) else Color(0xFFB3261E)
    val charge = if (dark) Color(0xFF69D447) else Color(0xFF3AAA35)
    val bolt = Color(0xFF0E1A12)
    val boltEdge = if (dark) Color(0xFFF1F9E9) else Color(0xFFF6FBEF)

    val selectedCheck: Color get() = success
    val connected: Color get() = success
}

private val TONES: Map<Pair<AndpopTint, Boolean>, Tones> = mapOf(
    AndpopTint.Wallpaper to true to Tones(0xFF0C1015, 0xFF141A20, 0xFF1C232A, 0xFF252C35, 0xFF2E3640, 0xFFEFF2F6, 0xFFB7C0CB, 0xFF939CA8, 0xFF99C8FE, 0xFF091E34, 0xFF213B57, 0xFFD6E8FE, 0xFF1A2F46),
    AndpopTint.Wallpaper to false to Tones(0xFFF5F8FC, 0xFFFFFFFF, 0xFFEAF0F7, 0xFFDEE6EF, 0xFFD2DCE7, 0xFF12181E, 0xFF47515C, 0xFF5F6973, 0xFF1566B1, 0xFFFFFFFF, 0xFFD5E8FF, 0xFF052C51, 0xFFD6E8FE),
    AndpopTint.Glacier to true to Tones(0xFF0A1113, 0xFF111B1D, 0xFF192427, 0xFF202E31, 0xFF29383C, 0xFFEDF3F4, 0xFFB3C3C6, 0xFF8E9FA3, 0xFF68D6EC, 0xFF002228, 0xFF00414B, 0xFFC9EEF6, 0xFF04343C),
    AndpopTint.Glacier to false to Tones(0xFFF3F9FA, 0xFFFFFFFF, 0xFFE7F1F3, 0xFFDBE8EB, 0xFFCEDEE2, 0xFF0F191B, 0xFF425357, 0xFF5B6B6E, 0xFF007182, 0xFFFFFFFF, 0xFFC5EFF8, 0xFF01323A, 0xFFC9EEF6),
    AndpopTint.Iris to true to Tones(0xFF100F15, 0xFF191820, 0xFF22212A, 0xFF2B2A34, 0xFF353440, 0xFFF2F1F6, 0xFFBFBDCA, 0xFF9B99A7, 0xFFC5B9FE, 0xFF1E1832, 0xFF3A3356, 0xFFE6E2FE, 0xFF2E2945),
    AndpopTint.Iris to false to Tones(0xFFF8F7FC, 0xFFFFFFFF, 0xFFEFEEF6, 0xFFE5E4EF, 0xFFDBD9E6, 0xFF17161E, 0xFF504E5B, 0xFF676672, 0xFF6852AD, 0xFFFFFFFF, 0xFFE6E2FF, 0xFF2D224F, 0xFFE6E2FE),
    AndpopTint.Guava to true to Tones(0xFF150E0F, 0xFF201718, 0xFF2A1F20, 0xFF342829, 0xFF3F3133, 0xFFF6F0F1, 0xFFCABBBD, 0xFFA79699, 0xFFFEA8B7, 0xFF301218, 0xFF532B33, 0xFFFEDCE1, 0xFF432329),
    AndpopTint.Guava to false to Tones(0xFFFCF6F7, 0xFFFFFFFF, 0xFFF6ECEE, 0xFFEFE1E3, 0xFFE6D6D8, 0xFF1E1416, 0xFF5B4B4D, 0xFF726365, 0xFFA33A55, 0xFFFFFFFF, 0xFFFFDCE1, 0xFF4B1624, 0xFFFEDCE1),
    AndpopTint.Sage to true to Tones(0xFF0C110E, 0xFF141C17, 0xFF1C251F, 0xFF242E27, 0xFF2D3931, 0xFFEFF3F0, 0xFFB7C3BA, 0xFF929F96, 0xFF8DD8A9, 0xFF062313, 0xFF1D422D, 0xFFD3EEDC, 0xFF183524),
    AndpopTint.Sage to false to Tones(0xFFF5F9F6, 0xFFFFFFFF, 0xFFEAF1EC, 0xFFDEE8E1, 0xFFD2DED6, 0xFF111914, 0xFF46544B, 0xFF5F6B63, 0xFF057846, 0xFFFFFFFF, 0xFFD0EFDA, 0xFF00351C, 0xFFD3EEDC),
    AndpopTint.Apricot to true to Tones(0xFF140F0B, 0xFF1F1813, 0xFF29201A, 0xFF332922, 0xFF3E332B, 0xFFF5F1EE, 0xFFC9BDB4, 0xFFA5998F, 0xFFF8B37F, 0xFF2E1603, 0xFF503117, 0xFFFCE0CD, 0xFF402713),
    AndpopTint.Apricot to false to Tones(0xFFFBF7F4, 0xFFFFFFFF, 0xFFF5EDE8, 0xFFEDE3DC, 0xFFE4D8CF, 0xFF1D1510, 0xFF594D44, 0xFF71655C, 0xFF965002, 0xFFFFFFFF, 0xFFFEE0C9, 0xFF442101, 0xFFFCE0CD),
    AndpopTint.Graphite to true to Tones(0xFF101010, 0xFF191919, 0xFF222222, 0xFF2B2B2B, 0xFF353535, 0xFFF2F2F2, 0xFFBFBFBF, 0xFF9B9B9B, 0xFFE8E8E8, 0xFF121212, 0xFF3D3D3D, 0xFFE6E6E6, 0xFF2E2E2E),
    AndpopTint.Graphite to false to Tones(0xFFF8F8F8, 0xFFFFFFFF, 0xFFEFEFEF, 0xFFE5E5E5, 0xFFDBDBDB, 0xFF171717, 0xFF505050, 0xFF676767, 0xFF1F1F1F, 0xFFFFFFFF, 0xFFDEDEDE, 0xFF2B2B2B, 0xFFE6E6E6),
)

/** Static tones for a theme. Wallpaper returns its blue sample; [andpopPalette] swaps in dynamic colors. */
internal fun andpopTones(tint: AndpopTint, dark: Boolean): Tones = TONES.getValue(tint to dark)

/** Tones taken from a Material You dynamic scheme, used for the Wallpaper theme. */
internal fun dynamicTones(scheme: ColorScheme): Tones {
    fun argb(color: Color): Long = color.toArgb().toLong() and 0xFFFFFFFFL
    return with(scheme) {
        Tones(
            argb(surface), argb(surfaceContainerLow), argb(surfaceContainer), argb(surfaceContainerHigh),
            argb(surfaceContainerHighest), argb(onSurface), argb(onSurfaceVariant), argb(outline),
            argb(primary), argb(onPrimary), argb(primaryContainer), argb(onPrimaryContainer), argb(primaryContainer),
        )
    }
}

/**
 * Maps the tonal roles onto Material roles. Legacy LibrePods screens paint their page with
 * surfaceContainer and their rows with surface, so those two map to the page and card tones.
 */
internal fun andpopPalette(tint: AndpopTint, dark: Boolean, tones: Tones = andpopTones(tint, dark)): AndpopPalette {
    val t = tones
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val error = if (dark) Color(0xFFFFB4AB) else Color(0xFFB3261E)
    val scheme = base.copy(
        primary = t.pri, onPrimary = t.onPri, primaryContainer = t.pc, onPrimaryContainer = t.onPc,
        inversePrimary = t.pc,
        secondary = t.pri, onSecondary = t.onPri, secondaryContainer = t.pc, onSecondaryContainer = t.onPc,
        tertiary = t.pri, onTertiary = t.onPri, tertiaryContainer = t.pc, onTertiaryContainer = t.onPc,
        background = t.bg, onBackground = t.on,
        surface = t.s1, onSurface = t.on, surfaceVariant = t.s2, onSurfaceVariant = t.on2,
        surfaceTint = t.pri, inverseSurface = t.on, inverseOnSurface = t.bg,
        error = error,
        outline = t.on3, outlineVariant = t.on.copy(alpha = 0.14f).compositeOver(t.s1),
        surfaceBright = t.s1, surfaceDim = t.bg,
        surfaceContainerLowest = t.s1, surfaceContainerLow = t.s1, surfaceContainer = t.bg,
        surfaceContainerHigh = t.s2, surfaceContainerHighest = t.s3,
    )
    return AndpopPalette(scheme, dark, t)
}

internal val DarkAndpopPalette = andpopPalette(AndpopTint.Default, true)
internal val LocalAndpopPalette = staticCompositionLocalOf { DarkAndpopPalette }
internal val LocalAndpopTint = staticCompositionLocalOf { AndpopTint.Default }
internal val LocalSetAndpopTint = staticCompositionLocalOf<(AndpopTint) -> Unit> { {} }
val LocalAndpopAppearance = staticCompositionLocalOf { AndpopAppearance.System }
val LocalSetAndpopAppearance = staticCompositionLocalOf<(AndpopAppearance) -> Unit> { {} }

/** Tonal card surface with a hairline edge. Replaces the v1.5 glass gradient. */
internal fun Modifier.devicePanel(
    palette: AndpopPalette,
    shape: Shape = RoundedCornerShape(24.dp),
    color: Color = palette.s1,
) = clip(shape)
    .background(color)
    .border(1.dp, palette.on.copy(alpha = 0.07f), shape)
