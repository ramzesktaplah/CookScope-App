package com.rork.cookscope.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rork.cookscope.data.ThemeManager

private val CookScopeDarkColors = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = BlackBg,
    primaryContainer = GoldDeep,
    onPrimaryContainer = TextPrimary,
    secondary = GoldLight,
    onSecondary = BlackBg,
    secondaryContainer = BlackElevated,
    onSecondaryContainer = GoldPrimary,
    tertiary = GoldMuted,
    onTertiary = BlackBg,
    tertiaryContainer = BlackCard,
    onTertiaryContainer = TextPrimary,
    background = BlackBg,
    onBackground = TextPrimary,
    surface = BlackSurface,
    onSurface = TextPrimary,
    surfaceVariant = BlackElevated,
    onSurfaceVariant = TextSecondary,
    outline = BlackBorder,
    outlineVariant = BlackBorderLight,
    inverseSurface = CreamSurface,
    inverseOnSurface = BlackBg,
)

private val CookScopeLightColors = lightColorScheme(
    primary = GoldDeep,
    onPrimary = Color.White,
    primaryContainer = GoldPrimary,
    onPrimaryContainer = BlackBg,
    secondary = GoldLight,
    onSecondary = BlackBg,
    secondaryContainer = CreamCard,
    onSecondaryContainer = GoldDeep,
    tertiary = GoldMuted,
    onTertiary = Color.White,
    tertiaryContainer = AmberLight,
    onTertiaryContainer = BlackBg,
    background = Color(0xFFFAF8F0),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFF5F2E8),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFEDE8D8),
    onSurfaceVariant = Color(0xFF5A5A5A),
    outline = Color(0xFFD4CFB8),
    outlineVariant = Color(0xFFB0AC98),
    inverseSurface = Charcoal,
    inverseOnSurface = Color(0xFFF5F2E8),
)

private val CookScopeTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.5).sp,
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-1).sp,
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    val isDarkMode = ThemeManager.isDarkMode.collectAsState().value
    val colorScheme = if (isDarkMode) CookScopeDarkColors else CookScopeLightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = CookScopeTypography,
        content = content
    )
}
