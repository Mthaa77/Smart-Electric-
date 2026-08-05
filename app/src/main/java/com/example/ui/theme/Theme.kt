package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PrimarySkyBlue,
    onPrimary = OnPrimaryWhite,
    primaryContainer = PrimarySkyContainer,
    onPrimaryContainer = OnPrimarySkyContainer,
    secondary = SecondaryEmerald,
    onSecondary = Color.White,
    secondaryContainer = SecondaryEmeraldContainer,
    onSecondaryContainer = OnSecondaryEmeraldContainer,
    tertiary = TertiaryIndigo,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryIndigoContainer,
    onTertiaryContainer = OnTertiaryIndigoContainer,
    background = LightBackground,
    onBackground = OnSurfaceDark,
    surface = LightSurface,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantSlate,
    onSurfaceVariant = OnSurfaceVariantMuted,
    outline = OutlineSoftBorder,
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2),
    onError = Color.White,
    onErrorContainer = Color(0xFF991B1B)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF0369A1),
    primaryContainer = Color(0xFF075985),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF34D399),
    onSecondary = Color(0xFF065F46),
    secondaryContainer = Color(0xFF047857),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFF818CF8),
    onTertiary = Color(0xFF3730A3),
    tertiaryContainer = Color(0xFF312E81),
    onTertiaryContainer = Color(0xFFEEF2FF),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = Color(0xFF475569),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF991B1B),
    onError = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFEE2E2)
)

@Composable
fun SmartElectricityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use custom electricity theme for branded consistency
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
