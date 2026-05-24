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

import androidx.compose.ui.graphics.Color

val M3PrimaryDark = Color(0xFF82D2FF)
val M3OnPrimaryDark = Color(0xFF00344A)
val M3PrimaryContainerDark = Color(0xFF004C6A)
val M3OnPrimaryContainerDark = Color(0xFFC7E7FF)

val M3SecondaryDark = Color(0xFFB4CAD6)
val M3OnSecondaryDark = Color(0xFF1E333C)
val M3SecondaryContainerDark = Color(0xFF354A53)
val M3OnSecondaryContainerDark = Color(0xFFD0E6F2)

val M3TertiaryDark = Color(0xFFC9C1EA)
val M3OnTertiaryDark = Color(0xFF312C4C)
val M3TertiaryContainerDark = Color(0xFF474264)
val M3OnTertiaryContainerDark = Color(0xFFE6DEFF)

val M3ErrorDark = Color(0xFFFFB4AB)
val M3OnErrorDark = Color(0xFF690005)
val M3ErrorContainerDark = Color(0xFF93000A)
val M3OnErrorContainerDark = Color(0xFFFFDAD6)

val M3BackgroundDark = Color(0xFF191C1E)
val M3OnBackgroundDark = Color(0xFFE1E2E4)
val M3SurfaceDark = Color(0xFF191C1E)
val M3OnSurfaceDark = Color(0xFFE1E2E4)
val M3SurfaceVariantDark = Color(0xFF40484C)
val M3OnSurfaceVariantDark = Color(0xFFBFC8CC)
val M3OutlineDark = Color(0xFF8A9296)

private val DarkColorScheme = darkColorScheme(
    primary = M3PrimaryDark,
    onPrimary = M3OnPrimaryDark,
    primaryContainer = M3PrimaryContainerDark,
    onPrimaryContainer = M3OnPrimaryContainerDark,
    secondary = M3SecondaryDark,
    onSecondary = M3OnSecondaryDark,
    secondaryContainer = M3SecondaryContainerDark,
    onSecondaryContainer = M3OnSecondaryContainerDark,
    tertiary = M3TertiaryDark,
    onTertiary = M3OnTertiaryDark,
    tertiaryContainer = M3TertiaryContainerDark,
    onTertiaryContainer = M3OnTertiaryContainerDark,
    error = M3ErrorDark,
    onError = M3OnErrorDark,
    errorContainer = M3ErrorContainerDark,
    onErrorContainer = M3OnErrorContainerDark,
    background = M3BackgroundDark,
    onBackground = M3OnBackgroundDark,
    surface = M3SurfaceDark,
    onSurface = M3OnSurfaceDark,
    surfaceVariant = M3SurfaceVariantDark,
    onSurfaceVariant = M3OnSurfaceVariantDark,
    outline = M3OutlineDark
)

val M3PrimaryLight = Color(0xFF00658B)
val M3OnPrimaryLight = Color(0xFFFFFFFF)
val M3PrimaryContainerLight = Color(0xFFC7E7FF)
val M3OnPrimaryContainerLight = Color(0xFF001E2E)

val M3SecondaryLight = Color(0xFF4C626B)
val M3OnSecondaryLight = Color(0xFFFFFFFF)
val M3SecondaryContainerLight = Color(0xFFD0E6F2)
val M3OnSecondaryContainerLight = Color(0xFF081E27)

val M3TertiaryLight = Color(0xFF5F5A7D)
val M3OnTertiaryLight = Color(0xFFFFFFFF)
val M3TertiaryContainerLight = Color(0xFFE6DEFF)
val M3OnTertiaryContainerLight = Color(0xFF1B1736)

val M3ErrorLight = Color(0xFFBA1A1A)
val M3OnErrorLight = Color(0xFFFFFFFF)
val M3ErrorContainerLight = Color(0xFFFFDAD6)
val M3OnErrorContainerLight = Color(0xFF410002)

val M3BackgroundLight = Color(0xFFFBFCFE)
val M3OnBackgroundLight = Color(0xFF191C1E)
val M3SurfaceLight = Color(0xFFFBFCFE)
val M3OnSurfaceLight = Color(0xFF191C1E)
val M3SurfaceVariantLight = Color(0xFFDCE4E8)
val M3OnSurfaceVariantLight = Color(0xFF40484C)
val M3OutlineLight = Color(0xFF70787C)

private val LightColorScheme = lightColorScheme(
    primary = M3PrimaryLight,
    onPrimary = M3OnPrimaryLight,
    primaryContainer = M3PrimaryContainerLight,
    onPrimaryContainer = M3OnPrimaryContainerLight,
    secondary = M3SecondaryLight,
    onSecondary = M3OnSecondaryLight,
    secondaryContainer = M3SecondaryContainerLight,
    onSecondaryContainer = M3OnSecondaryContainerLight,
    tertiary = M3TertiaryLight,
    onTertiary = M3OnTertiaryLight,
    tertiaryContainer = M3TertiaryContainerLight,
    onTertiaryContainer = M3OnTertiaryContainerLight,
    error = M3ErrorLight,
    onError = M3OnErrorLight,
    errorContainer = M3ErrorContainerLight,
    onErrorContainer = M3OnErrorContainerLight,
    background = M3BackgroundLight,
    onBackground = M3OnBackgroundLight,
    surface = M3SurfaceLight,
    onSurface = M3OnSurfaceLight,
    surfaceVariant = M3SurfaceVariantLight,
    onSurfaceVariant = M3OnSurfaceVariantLight,
    outline = M3OutlineLight
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
