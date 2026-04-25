package com.denko.flowerclassifier.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = GoldPrimary,
    onPrimary = GoldOnPrimary,
    primaryContainer = GoldContainer,
    onPrimaryContainer = GoldOnContainer,
    secondary = PinkSecondary,
    onSecondary = PinkOnSecondary,
    secondaryContainer = PinkContainer,
    onSecondaryContainer = PinkOnContainer,
    background = CreamBackground,
    onBackground = OnBackground,
    surface = CardSurface
)

private val DarkColors = darkColorScheme(
    primary = GoldContainer,
    onPrimary = GoldOnContainer,
    primaryContainer = GoldPrimary,
    onPrimaryContainer = GoldContainer,
    secondary = PinkContainer,
    onSecondary = PinkOnContainer,
    secondaryContainer = PinkSecondary,
    surface = CardSurfaceDark
)

@Composable
fun FlowerClassifierTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,  // true за Android 12+ system colors, false за нашата палитра
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}