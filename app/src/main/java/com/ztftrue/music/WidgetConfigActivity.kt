package com.ztftrue.music

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import com.ztftrue.music.ui.widget.WidgetConfigContent
import com.ztftrue.music.utils.SharedPreferencesUtils

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set result to CANCELED so if the user backs out, the widget is not placed
        setResult(RESULT_CANCELED)

        intent?.extras?.let {
            appWidgetId = it.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        val initialColor = SharedPreferencesUtils.getWidgetBackground(
            this,
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) appWidgetId else null
        )
        val initialContrast = SharedPreferencesUtils.getWidgetTextContrast(
            this,
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) appWidgetId else null
        )

        // Read last played song info from shared preferences for realistic preview
        val widgetPrefs = getSharedPreferences("Widgets", Context.MODE_PRIVATE)
        val title = widgetPrefs.getString("title", null)
        val author = widgetPrefs.getString("author", null)
        val path = widgetPrefs.getString("path", null)
        val isPlaying = widgetPrefs.getBoolean("playingStatus", false)

        setContent {
            val darkTheme = isSystemInDarkTheme()
            val colorScheme = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (darkTheme) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
                }
                darkTheme -> darkColorScheme()
                else -> lightColorScheme()
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WidgetConfigContent(
                        initialColorHex = initialColor,
                        initialContrast = initialContrast,
                        canPinWidget = false, // already being configured by launcher
                        currentTrackName = title,
                        currentArtistName = author,
                        currentCoverPath = path,
                        isPlaying = isPlaying,
                        onSave = { colorHex, contrast ->
                            val targetId = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) appWidgetId else null
                            SharedPreferencesUtils.setWidgetBackground(this, colorHex, targetId)
                            SharedPreferencesUtils.setWidgetTextContrast(this, contrast, targetId)
                            SharedPreferencesUtils.setWidgetEnable(this, true)

                            val appWidgetManager = AppWidgetManager.getInstance(this)
                            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                                PlayMusicWidget.updateAppWidget(this, appWidgetManager, appWidgetId)
                                val resultValue = Intent().apply {
                                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                }
                                setResult(RESULT_OK, resultValue)
                            } else {
                                val ids = appWidgetManager.getAppWidgetIds(ComponentName(this, PlayMusicWidget::class.java))
                                for (id in ids) {
                                    PlayMusicWidget.updateAppWidget(this, appWidgetManager, id)
                                }
                                setResult(RESULT_OK)
                            }
                            finish()
                        },
                        onDismiss = {
                            finish()
                        }
                    )
                }
            }
        }
    }
}
