package com.example.mygymstatsapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Fonte,
    onPrimary = Charcoal,
    primaryContainer = FonteDeep,
    onPrimaryContainer = FonteGlow,
    secondary = Steel,
    onSecondary = Charcoal,
    secondaryContainer = IronHigh,
    onSecondaryContainer = Chalk,
    tertiary = Gold,
    onTertiary = Charcoal,
    error = Danger,
    onError = Charcoal,
    background = Charcoal,
    onBackground = Chalk,
    surface = Charcoal,
    onSurface = Chalk,
    surfaceVariant = Iron,
    onSurfaceVariant = Ash,
    surfaceContainerLowest = Charcoal,
    surfaceContainerLow = Soot,
    surfaceContainer = Soot,
    surfaceContainerHigh = Iron,
    surfaceContainerHighest = IronHigh,
    outline = Rivet,
    outlineVariant = IronHigh,
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFC2410C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9C7),
    onPrimaryContainer = Color(0xFF3A1100),
    secondary = Color(0xFF3F6A85),
    onSecondary = Color.White,
    secondaryContainer = LimestoneLowest,
    onSecondaryContainer = Ink,
    tertiary = Color(0xFF9A6B00),
    onTertiary = Color.White,
    error = Color(0xFFC62839),
    background = Limestone,
    onBackground = Ink,
    surface = Limestone,
    onSurface = Ink,
    surfaceVariant = LimestoneLow,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = LimestoneHigh,
    surfaceContainerLow = LimestoneHigh,
    surfaceContainer = LimestoneHigh,
    surfaceContainerHigh = LimestoneLow,
    surfaceContainerHighest = LimestoneLowest,
    outline = Color(0xFFB3A796),
    outlineVariant = LimestoneLowest,
)

/** Dark by default (in-gym use); light and system modes are selectable in the profile. */
@Composable
fun MyGymStatsAppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
