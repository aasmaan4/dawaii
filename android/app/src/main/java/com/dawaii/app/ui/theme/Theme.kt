package com.dawaii.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.dawaii.app.data.preferences.AppTheme

private val SakuraDarkColorScheme = darkColorScheme(
    primary = BlushPinkPrimary,
    onPrimary = DarkCharcoalBg,
    primaryContainer = BlushPinkContainer,
    onPrimaryContainer = BlushPinkLight,
    surface = DarkSurface,
    onSurface = SoftWhite,
    background = DarkCharcoalBg,
    onBackground = SoftWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = MutedGreyPink,
    outline = DarkBorder
)

private val MinimalDarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF262626),
    onPrimaryContainer = Color(0xFFE5E5E5),
    surface = Color(0xFF141414),
    onSurface = Color(0xFFEEEEEE),
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFEEEEEE),
    surfaceVariant = Color(0xFF1F1F1F),
    onSurfaceVariant = Color(0xFFA0A0A0),
    outline = Color(0xFF333333)
)

@Composable
fun DawaiiTheme(
    appTheme: AppTheme = AppTheme.SAKURA,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.SAKURA -> SakuraDarkColorScheme
        AppTheme.NORMAL -> MinimalDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DawaiiTypography,
        content = content
    )
}
