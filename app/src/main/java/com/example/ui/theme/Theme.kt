package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val CapodDarkColorScheme = darkColorScheme(
    primary = CapodBlueBright,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1B3A75),
    onPrimaryContainer = Color(0xFFD8E6FF),
    secondary = CapodMint,
    onSecondary = Color(0xFF042F17),
    secondaryContainer = Color(0xFF143322),
    onSecondaryContainer = Color(0xFFBBF7D0),
    tertiary = ChargingAmber,
    onTertiary = ChargingAmberText,
    tertiaryContainer = ChargingAmberContainerDark,
    onTertiaryContainer = ChargingAmberTextDark,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceContainer,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = Color(0xFF333C4D),
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = BatteryLowRed,
    onError = Color.White,
)

private val CapodLightColorScheme = lightColorScheme(
    primary = CapodBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE8FF),
    onPrimaryContainer = Color(0xFF0B2A6B),
    secondary = CapodMintDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCFCE7),
    onSecondaryContainer = Color(0xFF052E16),
    tertiary = ChargingAmber,
    onTertiary = ChargingAmberText,
    tertiaryContainer = Color(0xFFFEF08A),
    onTertiaryContainer = ChargingAmberText,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceContainer,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = Color(0xFFDCE3F0),
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = BatteryLowRed,
    onError = Color.White,
)

val CapodShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun CapodTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> CapodDarkColorScheme
        else -> CapodLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CapodTypography,
        shapes = CapodShapes,
        content = content
    )
}
