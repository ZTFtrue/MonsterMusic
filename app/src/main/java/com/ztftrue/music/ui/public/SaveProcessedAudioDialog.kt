package com.ztftrue.music.ui.public

import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.sqlData.model.Auxr
import com.ztftrue.music.utils.AudioExportEngine
import com.ztftrue.music.utils.Utils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun SaveProcessedAudioDialog(
    musicViewModel: MusicViewModel,
    music: MusicItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var auxr by remember { mutableStateOf<Auxr?>(null) }
    var filename by remember { mutableStateOf("${music.name} (Processed)") }
    var selectedFormat by remember { mutableStateOf(AudioExportEngine.ExportFormat.WAV) }

    var isExporting by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var exportJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(music.id) {
        auxr = musicViewModel.getAuxrForTrack(context, music.id)
    }

    Dialog(
        onDismissRequest = {
            if (isExporting) {
                exportJob?.cancel()
            }
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
            dismissOnBackPress = !isExporting,
            dismissOnClickOutside = !isExporting
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp)
        ) {
            val currentAuxr = auxr
            if (currentAuxr == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Dialog Title
                    Text(
                        text = stringResource(R.string.export_audio),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Track info
                    Column {
                        Text(
                            text = music.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = music.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))

                    // Active Effects Summary
                    val activeEffects = remember(currentAuxr) {
                        getActiveEffectsSummary(currentAuxr)
                    }
                    Column {
                        Text(
                            text = if (activeEffects.isEmpty()) {
                                stringResource(R.string.no_effects_active)
                            } else {
                                stringResource(R.string.active_effects, activeEffects.joinToString(", "))
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Filename TextField
                    OutlinedTextField(
                        value = filename,
                        onValueChange = { if (!isExporting) filename = it },
                        label = {
                            Text(
                                text = stringResource(R.string.output_filename),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        singleLine = true,
                        enabled = !isExporting,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onBackground,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        )
                    )

                    // Format Selection
                    Column {
                        Text(
                            text = stringResource(R.string.export_format),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // WAV option
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isExporting) {
                                    selectedFormat = AudioExportEngine.ExportFormat.WAV
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedFormat == AudioExportEngine.ExportFormat.WAV,
                                onClick = { if (!isExporting) selectedFormat = AudioExportEngine.ExportFormat.WAV },
                                enabled = !isExporting,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onBackground
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.format_wav),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // M4A option
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isExporting) {
                                    selectedFormat = AudioExportEngine.ExportFormat.M4A
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedFormat == AudioExportEngine.ExportFormat.M4A,
                                onClick = { if (!isExporting) selectedFormat = AudioExportEngine.ExportFormat.M4A },
                                enabled = !isExporting,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onBackground
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.format_m4a),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    // Export progress
                    if (isExporting) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.exporting_audio),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (isExporting) {
                                    exportJob?.cancel()
                                }
                                onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = stringResource(R.string.cancel),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = {
                                val targetAuxr = currentAuxr
                                val exportFilename = filename.ifBlank { "${music.name} (Processed)" }
                                exportJob = coroutineScope.launch {
                                    isExporting = true
                                    progress = 0f
                                    try {
                                        AudioExportEngine.exportAudio(
                                            context = context,
                                            musicItem = music,
                                            auxr = targetAuxr,
                                            outputFileName = exportFilename,
                                            format = selectedFormat,
                                            onProgress = { p -> progress = p }
                                        )
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.export_success, "Music/MonsterMusic"),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        onDismiss()
                                    } catch (_: CancellationException) {
                                        // User cancelled export
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.export_failed, e.message ?: ""),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            },
                            enabled = !isExporting && filename.isNotBlank()
                        ) {
                            Text(stringResource(R.string.export_audio))
                        }
                    }
                }
            }
        }
    }
}

private fun getActiveEffectsSummary(auxr: Auxr): List<String> {
    val list = mutableListOf<String>()
    if (auxr.equalizer) {
        val preset = if (auxr.selectedPreset.isNotEmpty() && auxr.selectedPreset != Utils.custom) {
            auxr.selectedPreset
        } else {
            "Custom"
        }
        list.add("EQ ($preset)")
    }
    if (auxr.bassBoostEnabled && auxr.bassBoostStrength > 0) {
        list.add("Bass Boost (${auxr.bassBoostStrength / 10}%)")
    }
    if (auxr.virtualizerEnabled && auxr.virtualizerStrength > 0) {
        list.add("Virtualizer (${auxr.virtualizerStrength / 10}%)")
    }
    if (auxr.reverbEnabled) {
        list.add("Reverb")
    }
    if (auxr.echo) {
        list.add("Echo")
    }
    if (auxr.delayEnabled) {
        list.add("Delay")
    }
    if (auxr.chorusEnabled) {
        list.add("Chorus")
    }
    if (auxr.flangerEnabled) {
        list.add("Flanger")
    }
    if (auxr.polyphonyEnabled) {
        list.add("Polyphony")
    }
    if (auxr.speed != 1.0f) {
        list.add("Speed ${auxr.speed}x")
    }
    if (auxr.pitch != 1.0f) {
        list.add("Pitch ${auxr.pitch}x")
    }
    return list
}
