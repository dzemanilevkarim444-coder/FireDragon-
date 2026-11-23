package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = EmberPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF47190B),
    onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = AmberGold,
    onSecondary = Color(0xFF261900),
    secondaryContainer = Color(0xFF3E2D00),
    onSecondaryContainer = Color(0xFFFFE08D),
    tertiary = ShieldGreen,
    onTertiary = Color(0xFF00210D),
    tertiaryContainer = Color(0xFF0A381D),
    onTertiaryContainer = Color(0xFF9DF6B8),
    background = ObsidianBg,
    onBackground = ObsidianTextPrimary,
    surface = ObsidianSurface,
    onSurface = ObsidianTextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = ObsidianTextSecondary,
    surfaceContainerHigh = ObsidianElevated,
    outline = ObsidianOutline,
    error = CrimsonAccent,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = EmberPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBD0),
    onPrimaryContainer = Color(0xFF3A0B00),
    secondary = Color(0xFF9A6600),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE6B3),
    onSecondaryContainer = Color(0xFF2D1B00),
    tertiary = Color(0xFF15803D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF052E16),
    background = PaperBg,
    onBackground = PaperTextPrimary,
    surface = PaperSurface,
    onSurface = PaperTextPrimary,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = PaperTextSecondary,
    surfaceContainerHigh = PaperElevated,
    outline = PaperOutline,
    error = Color(0xFFDC2626),
    onError = Color.White
)

val FireDragonShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun FireDragonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = FireDragonTypography,
        shapes = FireDragonShapes,
        content = content
    )
}
