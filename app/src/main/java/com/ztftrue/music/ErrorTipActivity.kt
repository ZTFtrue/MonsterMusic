package com.ztftrue.music

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.ui.theme.MusicPitchTheme


class ErrorTipActivity : ComponentActivity() {
    private val musicViewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MusicPitchTheme(musicViewModel) {
                ErrorCollectorView(this@ErrorTipActivity)
            }
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    @Composable
    fun ErrorCollectorView(
        activity: ErrorTipActivity
    ) {
        var errorMessage by remember { mutableStateOf("") }
        LaunchedEffect(Unit) {
            try {
                val error: Throwable = intent.getSerializableExtra("error") as Throwable
                val sb = StringBuilder("")
                for (element in error.stackTrace) {
                    sb.append(element.toString())
                    sb.append("\n")
                }
                errorMessage = sb.toString()
            } catch (e: Exception) {
                Log.e("ERROR", e.toString())
            }
        }
        BackHandler(enabled = true) {

        }
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "MainView" },
            color = MaterialTheme.colorScheme.background
        ) {

            Scaffold(modifier = Modifier,
                topBar = {
                    Column {
                        Text(text = "Error tip", color = MaterialTheme.colorScheme.onBackground)
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(color = MaterialTheme.colorScheme.onBackground)
                        )
                    }
                }, bottomBar = {
                    Row {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND)
                                intent.setType("text/plain")
                                intent.putExtra(Intent.EXTRA_SUBJECT, "Crash report")
                                intent.putExtra(Intent.EXTRA_TEXT, errorMessage)
                                startActivity(
                                    Intent.createChooser(
                                        intent,
                                        "Copy"
                                    )
                                )
                                activity.startActivity(intent)
                            }
                        ) {
                            Text(
                                text = "Feed back"
                            )
                        }
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data =
                                        Uri.parse("mailto:") // only email apps should handle this
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf("ztftrue@gmail.com"))
                                    putExtra(Intent.EXTRA_SUBJECT, "Crash report")
                                    putExtra(Intent.EXTRA_TEXT, errorMessage)
                                }
                                activity.startActivity(intent)
                            }
                        ) {
                            Text(
                                text = "Send to email"
                            )
                        }
                    }

                }, content = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(it)
                    ) {
                        item(1) {
                            Text(
                                text = "Sorry some error happens, you can feedback it with this message.",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                })
        }

    }

}