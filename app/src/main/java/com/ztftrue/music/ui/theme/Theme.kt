package com.ztftrue.music.ui.theme

import android.app.Activity
import android.os.Build
import androidx.annotation.OptIn
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.media3.common.util.UnstableApi
import androidx.palette.graphics.Palette
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.utils.CustomColorUtils

/**
 * “On” colors are primarily applied to text, iconography,
 *  and strokes. Sometimes, they are applied to surfaces.
 */
private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
)

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)

@OptIn(UnstableApi::class)
@Composable
fun MusicPitchTheme(
    musicViewModel: MusicViewModel,
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = remember {
        mutableStateOf(LightColorScheme)
    }

    LaunchedEffect(
        musicViewModel.themeSelected.intValue,
        musicViewModel.currentPlay.value
    ) {
        // "Follow System", "Light", "Dark", "Follow Music Cover"
        if (musicViewModel.themeSelected.intValue == 0) {
            colorScheme.value =
                if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) if (darkTheme) dynamicDarkColorScheme(
                    context
                ) else dynamicLightColorScheme(context) else if (darkTheme) DarkColorScheme else LightColorScheme
        } else if (musicViewModel.themeSelected.intValue == 1) {
            colorScheme.value = LightColorScheme
        } else if (musicViewModel.themeSelected.intValue == 2) {
            colorScheme.value = DarkColorScheme
        } else {
            val bitmap = musicViewModel.getCurrentMusicCover()
            if (bitmap != null) {
                Palette.from(bitmap).generate { palette -> // 从 Palette 中获取颜色信息
                    if (palette != null) {
                        colorScheme.value = colorScheme.value.copy(
                            background = Color(palette.dominantSwatch?.rgb ?: 0),
                            surface = Color(palette.dominantSwatch?.rgb ?: 0),
                            primary = Color(palette.mutedSwatch?.rgb ?: 0),
                            onPrimary = Color(palette.mutedSwatch?.bodyTextColor ?: 0),
                            primaryContainer = Color(palette.mutedSwatch?.rgb ?: 0),
                            // slider uncovered and enable
//                            surfaceVariant = Color(palette.darkVibrantSwatch?.rgb ?: 0),
//                            onPrimaryContainer = Color(palette.vibrantSwatch?.bodyTextColor ?: 0),
                            secondary = Color(palette.darkVibrantSwatch?.rgb ?: 0),
                            onSecondary = Color(palette.darkVibrantSwatch?.bodyTextColor ?: 0),
                            // default icon color
                            onBackground = Color(palette.dominantSwatch?.bodyTextColor ?: 0),
                            onSurface = Color(palette.dominantSwatch?.bodyTextColor ?: 0),
                        )
                    }
                }
            } else {
                colorScheme.value =
                    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) if (darkTheme) dynamicDarkColorScheme(
                        context
                    ) else dynamicLightColorScheme(context) else if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }


    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.value.background.toArgb()
            window.navigationBarColor = colorScheme.value.background.toArgb()
            val darkColor = !CustomColorUtils.isColorDark(colorScheme.value.background.toArgb())
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkColor
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars =
                darkColor
        }
    }

    MaterialTheme(
        colorScheme = colorScheme.value,
        typography = Typography,
        content = content,
    )
}

