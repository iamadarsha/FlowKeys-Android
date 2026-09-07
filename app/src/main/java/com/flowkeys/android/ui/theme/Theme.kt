package com.flowkeys.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StitchDarkColorScheme = darkColorScheme(
    primary = StitchAccentCoral,
    secondary = StitchAccentPeach,
    tertiary = StitchSuccess,
    background = StitchCanvas,
    surface = StitchSurface1,
    surfaceVariant = StitchSurface2,
    onPrimary = StitchCanvas,
    onSecondary = StitchCanvas,
    onTertiary = StitchCanvas,
    onBackground = StitchTextPrimary,
    onSurface = StitchTextPrimary,
    onSurfaceVariant = StitchTextSecondary,
    outline = StitchTextMuted,
    error = StitchError
)

@Composable
fun FlowKeysTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StitchDarkColorScheme,
        content = content
    )
}
