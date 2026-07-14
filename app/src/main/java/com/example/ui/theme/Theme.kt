package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = CosmicNeonPurple,
    secondary = CosmicCyan,
    tertiary = CelestialGold,
    background = SpaceVoid,
    surface = DarkGreySpace,
    onPrimary = SpaceVoid,
    onSecondary = SpaceVoid,
    onTertiary = SpaceVoid,
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White
  )

private val LightColorScheme = DarkColorScheme // Force immersive space mode!

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force immersive dark mode
  dynamicColor: Boolean = false, // Disable dynamic colors to enforce space theme colors
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
