package com.ztftrue.music.ui.other

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.sqlData.model.MainTab
import com.ztftrue.music.ui.public.BackTopBar
import com.ztftrue.music.utils.LyricsSettings.FIRST_EMBEDDED_LYRICS
import com.ztftrue.music.utils.SharedPreferencesName.LYRICS_SETTINGS
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.Utils.openBrowser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 * show all music of playlist
 */
@UnstableApi
@Composable
fun SettingsPage(
    musicViewModel: MusicViewModel,
    navController: NavHostController,
) {
    val context = LocalContext.current

    var expanded by remember { mutableStateOf(false) }
    var preferEmbedded by remember {
        mutableStateOf(
            context.getSharedPreferences(
                LYRICS_SETTINGS, Context.MODE_PRIVATE
            )
                .getBoolean(FIRST_EMBEDDED_LYRICS, false)
        )
    }
    LaunchedEffect(Unit) {

    }

    Scaffold(
        modifier = Modifier.padding(all = 0.dp),
        topBar = {
            Column {
                BackTopBar(navController)
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color = MaterialTheme.colorScheme.onBackground)
                )
            }
        },
        bottomBar = { },
        floatingActionButton = {},
        content = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                item(1) {
                    val color = MaterialTheme.colorScheme.onBackground
                    var showDialog by remember { mutableStateOf(false) }
                    var showAboutDialog by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(0.dp)
                            .drawBehind {
                                drawLine(
                                    color = color,
                                    start = Offset(0f, size.height - 1.dp.toPx()),
                                    end = Offset(size.width, size.height - 1.dp.toPx()),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            .clickable {
                                showDialog = !showDialog
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (showDialog) {
                            ManageTabDialog(
                                musicViewModel,
                                onDismiss = {
                                    showDialog = false
                                })
                        }
                        Text(
                            text = "Manage tab items",
                            Modifier.padding(start = 10.dp),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(0.dp)
                            .drawBehind {
                                drawLine(
                                    color = color,
                                    start = Offset(0f, size.height - 1.dp.toPx()),
                                    end = Offset(size.width, size.height - 1.dp.toPx()),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            .clickable {

                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row {
                            Text(
                                text = "Prefer embedded lyrics",
                                Modifier.padding(start = 10.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Switch(
                                checked = preferEmbedded,
                                onCheckedChange = { value ->
                                    preferEmbedded = value
                                    context.getSharedPreferences(
                                        LYRICS_SETTINGS,
                                        Context.MODE_PRIVATE
                                    ).edit().putBoolean(FIRST_EMBEDDED_LYRICS, value).apply()
                                },
//                                colors = SwitchDefaults.colors(checkedThumbColor = Color.Green)
                            )
                        }

                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(0.dp)
                            .drawBehind {
                                drawLine(
                                    color = color,
                                    start = Offset(0f, size.height - 1.dp.toPx()),
                                    end = Offset(size.width, size.height - 1.dp.toPx()),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            .clickable {
                                showAboutDialog = !showAboutDialog
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (showAboutDialog) {
                            AboutDialog {
                                showAboutDialog = false
                            }
                        }
                        Text(
                            text = "About",
                            Modifier.padding(start = 10.dp),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(0.dp)
                            .clickable { expanded = !expanded }
                            .drawBehind {
                                drawLine(
                                    color = color,
                                    start = Offset(0f, size.height - 1.dp.toPx()),
                                    end = Offset(size.width, size.height - 1.dp.toPx()),
                                    strokeWidth = 1.dp.toPx()
                                )
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {

                        var selectedIndex by remember {
                            musicViewModel.themeSelected
                        }

                        val offset = remember { mutableIntStateOf(0) }
                        val selectedText = if (selectedIndex >= 0) {
                            Utils.items[selectedIndex]
                        } else {
                            ""
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Select theme:",
                                Modifier.padding(start = 10.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = selectedText,
                                Modifier.padding(end = 10.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.tertiaryContainer
                                ),
                            offset = DpOffset(
                                x = 0.dp,
                                y = with(LocalDensity.current) { offset.intValue.toDp() }
                            )
                        ) {
                            Utils.items.forEachIndexed { index, item ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            item,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    },
                                    onClick = {
                                        selectedIndex = index
                                        expanded = false
                                        context.getSharedPreferences(
                                            "SelectedTheme",
                                            Context.MODE_PRIVATE
                                        ).edit().putInt("SelectedTheme", index).apply()
                                    })
                            }
                        }
                    }
                }

            }

        },
    )


}


@UnstableApi
@Composable
fun ManageTabDialog(musicViewModel: MusicViewModel, onDismiss: () -> Unit) {

    val context = LocalContext.current
    val scopeMain = CoroutineScope(Dispatchers.IO)

    val mainTabList = remember { mutableStateListOf<MainTab>() }
    var size by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        scopeMain.launch {
            mainTabList.addAll(
                musicViewModel.getDb(context).MainTabDao().findAllMainTabSortByPriority()
                    ?: emptyList()
            )
            size = mainTabList.size
        }
    }
    fun onConfirmation() {
        musicViewModel.mainTabList.clear()
        mainTabList.forEach {
            if (it.isShow) {
                musicViewModel.mainTabList.add(it)
            }
        }
        scopeMain.launch {
            musicViewModel.getDb(context).MainTabDao().updateAll(mainTabList)
            onDismiss()
        }
    }


    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = true, dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "ManageTab", modifier = Modifier
                        .padding(2.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color = MaterialTheme.colorScheme.onBackground)
                )


                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(size) {
                        val item = mainTabList[it]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 10.dp, end = 10.dp
                                ),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        )
                        {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(
                                    modifier = Modifier.width(120.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (it != 0) {
                                        FilledIconButton(modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .semantics {
                                                contentDescription = "Up tab priority"
                                            },
                                            onClick = {
                                                item.priority = it - 1
                                                mainTabList.remove(item)
                                                mainTabList.add(it - 1, item)
                                            }) {
                                            Image(
                                                painter = painterResource(R.drawable.ic_up),
                                                contentDescription = "Up tab priority",
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape),
                                                colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape),
                                        )
                                    }
                                    Divider(
                                        color = Color.Transparent,
                                        modifier = Modifier
                                            .height(40.dp)
                                            .width(10.dp)
                                            .background(
                                                MaterialTheme.colorScheme.background
                                            )
                                    )
                                    if (it < mainTabList.size - 1) {
                                        FilledIconButton(modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .semantics {
                                                contentDescription = "Down tab priority"
                                            },
                                            onClick = {
                                                item.priority = it + 1
                                                mainTabList.remove(item)
                                                mainTabList.add(it + 1, item)
                                            }) {
                                            Image(
                                                painter = painterResource(R.drawable.ic_down),
                                                contentDescription = "Down tab priority",
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape),
                                                colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = item.name,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            var isChecked by remember {
                                mutableStateOf(false)
                            }
                            isChecked = item.isShow
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { v ->
                                    isChecked = v
                                    item.isShow = v
                                },
                                modifier = Modifier
                                    .padding(8.dp)
                                    .semantics {
                                        contentDescription = if (isChecked) {
                                            "Show this tab${item.name}"
                                        } else {
                                            "Hide this tab${item.name}"
                                        }
                                    }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { onDismiss() },
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(0.5f),
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onBackground)
                    }
                    Divider(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.onBackground)
                            .width(1.dp)
                            .height(50.dp)
                    )
                    TextButton(
                        onClick = {
                            onConfirmation()
                        },
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),

                        ) {
                        Text("Ok", color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }
    )
}


@Composable
fun AboutDialog(onDismiss: () -> Unit) {

    val context = LocalContext.current


    fun onConfirmation() {
        onDismiss()
    }

    val color = MaterialTheme.colorScheme.onBackground


    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = true, dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "About&Thanks", modifier = Modifier
                        .padding(2.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color = MaterialTheme.colorScheme.onBackground)
                )


                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(0.dp)
                                .drawBehind {
                                    drawLine(
                                        color = color,
                                        start = Offset(0f, size.height - 1.dp.toPx()),
                                        end = Offset(size.width, size.height - 1.dp.toPx()),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                                .clickable {
                                    openBrowser("https://github.com/ZTFtrue/MonsterMusic", context)
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "SourceCode",
                                Modifier.padding(start = 10.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(0.dp)
                                .drawBehind {
                                    drawLine(
                                        color = color,
                                        start = Offset(0f, size.height - 1.dp.toPx()),
                                        end = Offset(size.width, size.height - 1.dp.toPx()),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                                .clickable {
                                    openBrowser("https://github.com/JorenSix/TarsosDSP", context)
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "tarsos.dsp", Modifier.padding(start = 10.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(0.dp)
                                .drawBehind {
                                    drawLine(
                                        color = color,
                                        start = Offset(0f, size.height - 1.dp.toPx()),
                                        end = Offset(size.width, size.height - 1.dp.toPx()),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                                .clickable {
                                    openBrowser(
                                        "https://arachnoid.com/BiQuadDesigner/index.html",
                                        context
                                    )
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "BiQuadDesigner", Modifier.padding(start = 10.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(0.dp)
                                .drawBehind {
                                    drawLine(
                                        color = color,
                                        start = Offset(0f, size.height - 1.dp.toPx()),
                                        end = Offset(size.width, size.height - 1.dp.toPx()),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                                .clickable {
                                    openBrowser(
                                        "https://developer.android.com/guide/topics/media/media3",
                                        context
                                    )
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "Media3", Modifier.padding(start = 10.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(0.dp)
                                .drawBehind {
                                    drawLine(
                                        color = color,
                                        start = Offset(0f, size.height - 1.dp.toPx()),
                                        end = Offset(size.width, size.height - 1.dp.toPx()),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                                .clickable {
                                    openBrowser(
                                        "https://stackoverflow.com/questions/14269144/how-to-implement-an-equalizer",
                                        context
                                    )
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "Stackoverflow answer", Modifier.padding(start = 10.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = {
                            onConfirmation()
                        },
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                    ) {
                        Text("Ok", color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }
    )
}

