package com.ztftrue.music.ui.theme

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import com.ztftrue.music.ImageSource
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.utils.CustomColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = remember {
        mutableStateOf(LightColorScheme)
    }

    fun generateAndApplyColorScheme(
        bitmap: Bitmap,
        currentColorScheme: MutableState<ColorScheme>,
        defaultColorScheme: ColorScheme
    ) {
        Palette.from(bitmap).generate { palette ->
            if (palette == null) {
                Log.w(
                    "ColorPalette",
                    "Palette could not be generated from bitmap. Using default ColorScheme."
                )
                currentColorScheme.value = defaultColorScheme
                return@generate
            }

            val dominantColor =
                palette.dominantSwatch?.rgb ?: defaultColorScheme.background.toArgb()
            val primaryColor = palette.lightMutedSwatch?.rgb
                ?: palette.mutedSwatch?.rgb
                ?: palette.vibrantSwatch?.rgb
                ?: dominantColor
            val secondaryColor = palette.darkVibrantSwatch?.rgb
                ?: palette.vibrantSwatch?.rgb
                ?: primaryColor

            fun Palette.Swatch?.getTextColor(fallbackColor: Color): Color {
                return if (this != null && this.bodyTextColor != 0 && this.titleTextColor != 0) {
                    Color(this.bodyTextColor)
                } else {
                    fallbackColor
                }
            }

            currentColorScheme.value = currentColorScheme.value.copy(
                background = Color(dominantColor),
                surface = Color(dominantColor),
                onBackground = palette.dominantSwatch.getTextColor(defaultColorScheme.onBackground),
                onSurface = palette.dominantSwatch.getTextColor(defaultColorScheme.onSurface),
                primary = Color(primaryColor),
                onPrimary = palette.lightMutedSwatch.getTextColor(defaultColorScheme.onPrimary),
                primaryContainer = Color(
                    palette.lightMutedSwatch?.titleTextColor
                        ?: palette.mutedSwatch?.titleTextColor
                        ?: primaryColor
                ),
                onPrimaryContainer = palette.lightMutedSwatch.getTextColor(defaultColorScheme.onPrimaryContainer),

                secondary = Color(secondaryColor),
                onSecondary = palette.darkVibrantSwatch.getTextColor(defaultColorScheme.onSecondary),
                onSecondaryContainer = palette.lightVibrantSwatch.getTextColor(defaultColorScheme.onSecondaryContainer),
                surfaceVariant = Color(
                    palette.mutedSwatch?.rgb
                        ?: palette.lightMutedSwatch?.rgb
                        ?: defaultColorScheme.surfaceVariant.toArgb()
                ),
                onSurfaceVariant = palette.mutedSwatch.getTextColor(defaultColorScheme.onSurfaceVariant),

                tertiary = Color(
                    palette.vibrantSwatch?.rgb ?: defaultColorScheme.tertiary.toArgb()
                ),
                onTertiary = palette.vibrantSwatch.getTextColor(defaultColorScheme.onTertiary),
                error = defaultColorScheme.error, // 错误色通常不从图片提取，保持默认
                onError = defaultColorScheme.onError,
                outline = defaultColorScheme.outline,
                scrim = defaultColorScheme.scrim,
            )
        }
    }


    LaunchedEffect(
        musicViewModel.themeSelected.intValue,
        musicViewModel.needRefreshTheme.value
    ) {
        // "Follow System", "Light", "Dark", "Follow Music Cover","material you"
        if (musicViewModel.themeSelected.intValue == 0) {
            colorScheme.value = if (darkTheme) DarkColorScheme else LightColorScheme
        } else if (musicViewModel.themeSelected.intValue == 1) {
            colorScheme.value = LightColorScheme
        } else if (musicViewModel.themeSelected.intValue == 2) {
            colorScheme.value = DarkColorScheme
        } else if (musicViewModel.themeSelected.intValue == 3) {
            val cover = musicViewModel.currentMusicCover.value
            when (cover) {
                is ImageSource.Resource -> {
                    withContext(Dispatchers.IO) {
                        val bitmap = BitmapFactory.decodeResource(context.resources, cover.id)
                        if (bitmap != null) {
                            withContext(Dispatchers.Main) {
                                generateAndApplyColorScheme(
                                    bitmap,
                                    colorScheme,
                                    if (darkTheme) DarkColorScheme else LightColorScheme
                                )
                            }
                        }
                    }
                }

                is ImageSource.BitmapFile -> {
                    generateAndApplyColorScheme(
                        cover.bitmap,
                        colorScheme,
                        if (darkTheme) DarkColorScheme else LightColorScheme
                    )
                }

                is ImageSource.FilePath -> {
                    withContext(Dispatchers.IO) {
                        val bitmap = BitmapFactory.decodeFile(cover.path)
                        if (bitmap != null) {
                            withContext(Dispatchers.Main) {
                                generateAndApplyColorScheme(
                                    bitmap,
                                    colorScheme,
                                    if (darkTheme) DarkColorScheme else LightColorScheme
                                )
                            }
                        }
                    }
                }
            }
        } else if (musicViewModel.themeSelected.intValue == 4) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                colorScheme.value =
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(
                        context
                    )
            }
        }
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
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

