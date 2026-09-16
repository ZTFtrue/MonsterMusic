package com.ztftrue.music.ui.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.ztftrue.music.PlayMusicWidget
import com.ztftrue.music.R
import com.ztftrue.music.ui.other.MyAdvancedColorPicker
import com.ztftrue.music.utils.Utils
import kotlin.math.roundToInt

data class ColorPreset(val name: String, val hex: String)

@OptIn(ExperimentalStdlibApi::class)
@Composable
fun WidgetConfigContent(
    initialColorHex: String,
    initialContrast: String = "auto",
    canPinWidget: Boolean = false,
    currentTrackName: String? = null,
    currentArtistName: String? = null,
    currentCoverPath: String? = null,
    isPlaying: Boolean = false,
    onSave: (colorHex: String, contrast: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var currentColorHex by remember { mutableStateOf(initialColorHex) }
    var currentContrast by remember { mutableStateOf(initialContrast) }

    // Extract initial opacity (0f..1f) from initialColorHex
    val parsedInitialColor = remember(initialColorHex) {
        try {
            initialColorHex.toColorInt()
        } catch (_: Exception) {
            android.graphics.Color.WHITE
        }
    }
    var opacity by remember {
        mutableFloatStateOf(android.graphics.Color.alpha(parsedInitialColor) / 255f)
    }

    // Preset color list
    val presets = remember {
        listOf(
            ColorPreset(context.getString(R.string.widget_presets) + " 1 (Theme)", "#FF01579B"),
            ColorPreset(context.getString(R.string.widget_presets) + " 2 (Dark Glass)", "#80000000"),
            ColorPreset(context.getString(R.string.widget_presets) + " 3 (Light Glass)", "#80FFFFFF"),
            ColorPreset(context.getString(R.string.widget_presets) + " 4 (Transparent)", "#00000000"),
            ColorPreset(context.getString(R.string.widget_presets) + " 5 (Black)", "#FF000000"),
            ColorPreset(context.getString(R.string.widget_presets) + " 6 (White)", "#FFFFFFFF"),
            ColorPreset(context.getString(R.string.widget_presets) + " 7 (Teal)", "#FF004D40"),
            ColorPreset(context.getString(R.string.widget_presets) + " 8 (Purple)", "#FF311B92")
        )
    }

    // Cover bitmap
    var coverBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(currentCoverPath) {
        if (!currentCoverPath.isNullOrEmpty()) {
            coverBitmap = Utils.getCoverBitmap(context, currentCoverPath)
        }
    }

    // Helper to calculate effective ARGB hex string given a base color and opacity
    fun computeArgbHex(colorInt: Int, alphaFloat: Float): String {
        val a = (alphaFloat * 255f).roundToInt().coerceIn(0, 255)
        val r = android.graphics.Color.red(colorInt)
        val g = android.graphics.Color.green(colorInt)
        val b = android.graphics.Color.blue(colorInt)
        return String.format("#%02X%02X%02X%02X", a, r, g, b)
    }

    val parsedColorInt = remember(currentColorHex) {
        try {
            currentColorHex.toColorInt()
        } catch (_: Exception) {
            android.graphics.Color.WHITE
        }
    }

    val isDarkBackground = remember(parsedColorInt) {
        val alpha = android.graphics.Color.alpha(parsedColorInt)
        if (alpha < 64) {
            true // wallpaper transparent assumption
        } else {
            val r = android.graphics.Color.red(parsedColorInt)
            val g = android.graphics.Color.green(parsedColorInt)
            val b = android.graphics.Color.blue(parsedColorInt)
            val darkness = 1.0 - (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            darkness >= 0.45
        }
    }

    val effectiveUseLightText = when (currentContrast) {
        "light" -> true
        "dark" -> false
        else -> isDarkBackground
    }

    val previewTextColor = if (effectiveUseLightText) Color.White else Color(0xFF1C1B1F)
    val previewAuthorColor = if (effectiveUseLightText) Color.White.copy(alpha = 0.7f) else Color(0xFF79747E)
    val previewIconColor = if (effectiveUseLightText) Color.White else Color(0xFF1C1B1F)
    val previewBgColor = Color(parsedColorInt)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.widget_config_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

        // ==========================================
        // Live Widget Preview
        // ==========================================
        Text(
            text = stringResource(R.string.widget_preview),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = previewBgColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cover
                Box(
                    modifier = Modifier
                        .size(85.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    if (coverBitmap != null) {
                        Image(
                            bitmap = coverBitmap!!.asImageBitmap(),
                            contentDescription = stringResource(R.string.music_cover),
                            modifier = Modifier.size(85.dp)
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.songs_thumbnail_cover),
                            contentDescription = stringResource(R.string.music_cover),
                            modifier = Modifier.size(85.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Track Info & Controls
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (currentTrackName.isNullOrEmpty()) stringResource(R.string.app_name) else currentTrackName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = previewTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (currentArtistName.isNullOrEmpty()) stringResource(R.string.widget_no_track_playing) else currentArtistName,
                            style = MaterialTheme.typography.bodySmall,
                            color = previewAuthorColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.play_previous_song),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(previewIconColor),
                            modifier = Modifier.size(36.dp)
                        )
                        Image(
                            painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(previewIconColor),
                            modifier = Modifier.size(36.dp)
                        )
                        Image(
                            painter = painterResource(R.drawable.play_next_song),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(previewIconColor),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))

        // ==========================================
        // Opacity Slider
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.widget_opacity),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${(opacity * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = opacity,
            onValueChange = { newOpacity ->
                opacity = (newOpacity * 20f).roundToInt() / 20f
                currentColorHex = computeArgbHex(parsedColorInt, opacity)
            },
            valueRange = 0f..1f,
            steps = 19
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ==========================================
        // Presets Chips
        // ==========================================
        Text(
            text = stringResource(R.string.widget_presets),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val presetColorInt = try { preset.hex.toColorInt() } catch (_: Exception) { android.graphics.Color.WHITE }
                val isSelected = currentColorHex.equals(preset.hex, ignoreCase = true)

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(presetColorInt))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable {
                            currentColorHex = preset.hex
                            opacity = android.graphics.Color.alpha(presetColorInt) / 255f
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // Text & Icon Contrast
        // ==========================================
        Text(
            text = stringResource(R.string.widget_text_color),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val contrastOptions = listOf(
                "auto" to stringResource(R.string.widget_text_auto),
                "light" to stringResource(R.string.widget_text_light),
                "dark" to stringResource(R.string.widget_text_dark)
            )
            contrastOptions.forEach { (mode, label) ->
                val isSelected = currentContrast == mode
                FilterChip(
                    selected = isSelected,
                    onClick = { currentContrast = mode },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    border = null
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        // ==========================================
        // Custom Color Picker (Collapsible to prevent accidental touches)
        // ==========================================
        var showColorPicker by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { showColorPicker = !showColorPicker }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.custom),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = { showColorPicker = !showColorPicker },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text(
                    text = if (showColorPicker) "▲" else "▼",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        if (showColorPicker) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                MyAdvancedColorPicker(
                    onColorChanged = { envelope ->
                        val hexWithoutAlpha = envelope.hexCode.takeLast(6)
                        val rgbInt = try {
                            android.graphics.Color.parseColor("#$hexWithoutAlpha")
                        } catch (_: Exception) {
                            android.graphics.Color.WHITE
                        }
                        currentColorHex = computeArgbHex(rgbInt, opacity)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // ==========================================
        // Bottom Action Bar - ALWAYS DISPLAYED!
        // ==========================================
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Button(
                        onClick = {
                            onSave(currentColorHex, currentContrast)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.confirm),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                // Optional Pin to Home Screen button
                if (canPinWidget && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    if (appWidgetManager.isRequestPinAppWidgetSupported) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                onSave(currentColorHex, currentContrast)
                                val myProvider = ComponentName(context, PlayMusicWidget::class.java)
                                appWidgetManager.requestPinAppWidget(myProvider, null, null)
                                Toast.makeText(context, R.string.widget_pin_success, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.widget_add_to_home),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
