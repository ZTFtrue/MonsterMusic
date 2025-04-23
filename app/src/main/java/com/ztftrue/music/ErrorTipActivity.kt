package com.ztftrue.music

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.ui.theme.MusicPitchTheme
import kotlin.system.exitProcess


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

    @SuppressLint("UseKtx")
    @androidx.annotation.OptIn(UnstableApi::class)
    @Composable
    fun ErrorCollectorView(
        activity: ErrorTipActivity
    ) {
        var errorMessage by remember { mutableStateOf("") }
        LaunchedEffect(Unit) {
            try {
                errorMessage = intent.getStringExtra("error") ?: ""
                errorMessage =
                    "If possible, please tell me what caused this error to occur, or when the BUG occurs, you are clicking on the button of the app.   \n\n $errorMessage"
                errorMessage = errorMessage + "\n\nVERSION_CODE   " + BuildConfig.VERSION_CODE
                errorMessage = errorMessage + "\n\nBrand:   " + Build.BRAND
                errorMessage = errorMessage + "\n\nAndroidVersion:   " + Build.VERSION.SDK_INT
                errorMessage = errorMessage + "\n\n:DEVICE:   " + Build.DEVICE
            } catch (e: Exception) {
//                Log.e("ERROR", e.toString())
            }
        }
        BackHandler(enabled = true) {
            finish()
            exitProcess(0)
        }


        Scaffold(
            modifier = Modifier.padding(all = 0.dp),
            topBar = {
                Column {
                    Text(
                        text = stringResource(R.string.error_tip),
                        color = MaterialTheme.colorScheme.onBackground
                    )
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
                            text = stringResource(R.string.feedback),
                            color = MaterialTheme.colorScheme.onBackground
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
                            text = stringResource(R.string.send_email),
                            color = MaterialTheme.colorScheme.onBackground
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
                            text = stringResource(R.string.sorry_some_error_happens_you_can_feedback_it_with_this_message),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        SelectionContainer(
                            modifier = Modifier,
                            content = {
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        )
                    }
                }
            })


    }

}