package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4C4DFF), // Blue/Indigo default
    secondary = Zinc400,
    tertiary = Purple600,
    background = ElegantBackground,
    surface = Zinc900,
    surfaceVariant = Zinc800,
    onPrimary = Zinc100,
    onSecondary = Zinc100,
    onTertiary = Zinc100,
    onBackground = Zinc100,
    onSurface = Zinc100,
    onSurfaceVariant = Zinc400,
    outline = Zinc800
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    themeColorIndex: Int = 0, // Ignored now
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = DarkColorScheme, typography = Typography, content = content)
}
