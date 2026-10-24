package com.elyedali.debttracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Money you will receive. */
val GetColor = Color(0xFF1E9E55)

/** Money you owe / gave out. */
val GiveColor = Color(0xFFE5484D)

private val Light = lightColorScheme(
    primary = Color(0xFF146C43),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8F0D4),
    onPrimaryContainer = Color(0xFF002111),
    secondary = Color(0xFF4F6354),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2E8D5),
    onSecondaryContainer = Color(0xFF0C1F13),
    background = Color(0xFFF6FAF6),
    onBackground = Color(0xFF181D19),
    surface = Color(0xFFF6FAF6),
    onSurface = Color(0xFF181D19),
    surfaceVariant = Color(0xFFDDE5DB),
    onSurfaceVariant = Color(0xFF414941),
    surfaceContainerLow = Color(0xFFEEF4EE),
    surfaceContainer = Color(0xFFE8EFE8),
    outline = Color(0xFF717970),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF7DDA9F),
    onPrimary = Color(0xFF003920),
    primaryContainer = Color(0xFF005231),
    onPrimaryContainer = Color(0xFF99F7B9),
    secondary = Color(0xFFB6CCB9),
    onSecondary = Color(0xFF213527),
    secondaryContainer = Color(0xFF374B3D),
    onSecondaryContainer = Color(0xFFD2E8D5),
    background = Color(0xFF101510),
    onBackground = Color(0xFFDFE4DD),
    surface = Color(0xFF101510),
    onSurface = Color(0xFFDFE4DD),
    surfaceVariant = Color(0xFF414941),
    onSurfaceVariant = Color(0xFFC1C9BF),
    surfaceContainerLow = Color(0xFF181D18),
    surfaceContainer = Color(0xFF1C211C),
    outline = Color(0xFF8B938A),
)

@Composable
fun DebtTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content,
    )
}
