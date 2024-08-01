package com.ztftrue.music.ui.other

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import com.ztftrue.music.utils.model.FolderList
import com.ztftrue.music.utils.trackManager.FolderManger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.internal.toLongOrDefault


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
    var durationValue by remember { mutableStateOf("0") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var expanded by remember { mutableStateOf(false) }
    var showClearAlbumCache by remember { mutableStateOf(false) }
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
                BackTopBar(navController, stringResource(id = R.string.settings))
                HorizontalDivider(
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
                    var showManageFolderDialog by remember { mutableStateOf(false) }
                    var showAboutDialog by remember { mutableStateOf(false) }
                    var showSetListIndicatorDialog by remember { mutableStateOf(false) }

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
                            text = stringResource(R.string.manage_tab_items),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.prefer_embedded_lyrics),
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

                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
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
                                    showSetListIndicatorDialog = !showSetListIndicatorDialog
                                },
                        ) {

                            if (showSetListIndicatorDialog) {
                                SetListIndicatorDialog(
                                    onDismiss = {
                                        showSetListIndicatorDialog = false
                                    })
                            }
                            Text(
                                text = "Set list indicator",
                                Modifier.padding(start = 10.dp),
                                color = MaterialTheme.colorScheme.onBackground
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

                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
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
                                    showManageFolderDialog = !showManageFolderDialog
                                },
                        ) {

                            if (showManageFolderDialog) {
                                ManageFolderDialog(
                                    musicViewModel,
                                    onDismiss = {
                                        showManageFolderDialog = false
                                    })
                            }
                            Text(
                                text = "Manage ignore folder",
                                Modifier.padding(start = 10.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )


                        }

                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
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
                        Column {

                            Text(
                                text = stringResource(R.string.ignore_tracks_duration_less_than),
                                Modifier.padding(start = 10.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            CompositionLocalProvider(
                                LocalContentColor provides MaterialTheme.colorScheme.onBackground
                            ) {
                                OutlinedTextField(
                                    value = durationValue,
                                    onValueChange = { s ->
                                        val newText = s.ifEmpty {
                                            "0"
                                        }

                                        if (!newText.contains(".") && newText.toLongOrNull() != null) {
                                            durationValue = newText
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions.Default.copy(
                                        imeAction = ImeAction.Done,
                                        keyboardType = KeyboardType.Number
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            val sharedPreferences =
                                                context.getSharedPreferences(
                                                    "scan_config",
                                                    Context.MODE_PRIVATE
                                                )
                                            // -1 don't ignore any,0 ignore duration less than or equal 0s,
                                            sharedPreferences.edit().putLong(
                                                "ignore_duration",
                                                durationValue.toLong()
                                            ).apply()
                                            Toast.makeText(
                                                context,
                                                context.getString(
                                                    R.string.ignore_tracks_duration_less_than_s_set_successfully_please_restart_the_app_to_take_effect,
                                                    durationValue
                                                ),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            focusRequester.freeFocus()
                                            keyboardController?.hide()
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .background(MaterialTheme.colorScheme.primary),
                                    colors = TextFieldDefaults.colors(
                                        errorTextColor = MaterialTheme.colorScheme.primary,
                                        focusedTextColor = MaterialTheme.colorScheme.primary,
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.38f
                                        ),
                                        unfocusedTextColor = MaterialTheme.colorScheme.primary,
                                        focusedContainerColor = MaterialTheme.colorScheme.background,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                        cursorColor = MaterialTheme.colorScheme.primary,
                                        errorCursorColor = MaterialTheme.colorScheme.error,
                                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.38f
                                        ),
                                        disabledIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.12f
                                        ),
                                        errorIndicatorColor = MaterialTheme.colorScheme.error,
                                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.38f
                                        ),
                                        errorLeadingIconColor = MaterialTheme.colorScheme.error,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.38f
                                        ),
                                        errorTrailingIconColor = MaterialTheme.colorScheme.error,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.38f
                                        ),
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.38f
                                        ),
                                        errorLabelColor = MaterialTheme.colorScheme.error,
                                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.38f
                                        )
                                    ),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                                    suffix = {
                                        Text(
                                            text = "s",
                                            Modifier.padding(start = 10.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                )
                            }
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
                            text = stringResource(R.string.about),
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
                            R.string.app_name
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.select_theme),
                                Modifier.padding(start = 10.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(id = selectedText),
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
                                            stringResource(id = item),
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
                                showClearAlbumCache = true
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (showClearAlbumCache) {
                            ClearAlbumCoverDialog(musicViewModel = musicViewModel) {
                                showClearAlbumCache = false
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Clear Album Cover Cache",
                                Modifier.padding(start = 10.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
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
                    text = stringResource(R.string.manage_tab_items), modifier = Modifier
                        .padding(2.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )

                HorizontalDivider(
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
                                    HorizontalDivider(
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
                                    text = stringResource(
                                        id = Utils.translateMap[item.name] ?: R.string.app_name
                                    ),
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
                        Text(
                            stringResource(R.string.cancel),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    HorizontalDivider(
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
                        Text(
                            stringResource(id = R.string.confirm),
                            color = MaterialTheme.colorScheme.onBackground
                        )
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
                    text = stringResource(id = R.string.about_thanks), modifier = Modifier
                        .padding(2.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )

                HorizontalDivider(
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
                                text = stringResource(R.string.sourcecode),
                                modifier = Modifier.padding(start = 10.dp),
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
                        Text(
                            stringResource(id = R.string.confirm),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    )
}

@UnstableApi
@Composable
fun ManageFolderDialog(musicViewModel: MusicViewModel, onDismiss: () -> Unit) {

    val context = LocalContext.current
    val scopeMain = CoroutineScope(Dispatchers.IO)

    val folderList = remember { mutableStateListOf<FolderList>() }

    LaunchedEffect(Unit) {
        scopeMain.launch {
            val sharedPreferences =
                context.getSharedPreferences("scan_config", Context.MODE_PRIVATE)
            // -1 don't ignore any,0 ignore duration less than or equal 0s,
            val ignoreFolders = sharedPreferences.getString("ignore_folders", "")
            val folderMap: HashMap<Long, FolderList> = FolderManger.getMusicFolders(context)
            if (!ignoreFolders.isNullOrEmpty()) {
                ignoreFolders.split(",").forEach {
                    if (it.isNotEmpty()) {
                        folderMap[it.toLongOrDefault(-1)]?.isShow = false
                    }
                }
            }
            folderList.addAll(folderMap.values)
        }
    }
    fun onConfirmation() {
        val hideFolderIds = StringBuilder()
        folderList.forEach {
            if (!it.isShow) {
                if (hideFolderIds.isNotEmpty()) {
                    hideFolderIds.append(",").append(it.id)
                } else {
                    hideFolderIds.append(it.id)
                }
            }
        }
        val sharedPreferences = context.getSharedPreferences("scan_config", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("ignore_folders", hideFolderIds.toString()).apply()
        scopeMain.launch {
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
                    .fillMaxHeight()
                    .background(color = MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = MaterialTheme.colorScheme.background),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(color = MaterialTheme.colorScheme.onBackground)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    )
                    {
                        Text(
                            text = stringResource(R.string.check_to_not_ignore_the_folder_you_need_restart_the_app_to_take_effect),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(folderList.size) {
                            val item = folderList[it]
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
                        Text(
                            stringResource(R.string.cancel),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    HorizontalDivider(
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
                        Text(
                            stringResource(id = R.string.confirm),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    )
}

@UnstableApi
@Composable
fun ClearAlbumCoverDialog(musicViewModel: MusicViewModel, onDismiss: () -> Unit) {

    val context = LocalContext.current
    val scopeMain = CoroutineScope(Dispatchers.IO)


    LaunchedEffect(Unit) {

    }
    fun onConfirmation() {
        Utils.clearAlbumCoverCache(context)
        scopeMain.launch {
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
                    text = "Are you sure you want to clear album cover cache? this will take some time when next open album list.",
                    modifier = Modifier
                        .padding(2.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color = MaterialTheme.colorScheme.onBackground)
                )
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
                        Text(
                            stringResource(R.string.cancel),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    HorizontalDivider(
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
                        Text(
                            stringResource(id = R.string.confirm),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    )
}

@UnstableApi
@Composable
fun SetListIndicatorDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = CoroutineScope(Dispatchers.IO)
    var showSlideIndicator by remember { mutableStateOf(false) }
    var showTopIndicator by remember { mutableStateOf(false) }
    val sharedPreferences =
        context.getSharedPreferences("list_indicator_config", Context.MODE_PRIVATE)
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            showSlideIndicator = sharedPreferences.getBoolean("show_slide_indicator", true)
            showTopIndicator = sharedPreferences.getBoolean("show_top_indicator", true)
        }
    }
    @SuppressLint("ApplySharedPref")
    fun onConfirmation() {
        coroutineScope.launch {
            sharedPreferences.edit()
                .putBoolean("show_slide_indicator", showSlideIndicator)
                .putBoolean("show_top_indicator", showTopIndicator).apply()
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
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = MaterialTheme.colorScheme.background),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(color = MaterialTheme.colorScheme.onBackground)
                    )
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        item {
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
                                    Text(
                                        text = "Show slide indicator",
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                Checkbox(
                                    checked = showSlideIndicator,
                                    onCheckedChange = { v ->
                                        showSlideIndicator = v
                                    },
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .semantics {
                                            contentDescription = if (showSlideIndicator) {
                                                "Show slide indicator"
                                            } else {
                                                "Hide slide indicator"
                                            }
                                        }
                                )
                            }
                        }
                        item {
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
                                    Text(
                                        text = "Show top indicator",
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                Checkbox(
                                    checked = showTopIndicator,
                                    onCheckedChange = { v ->
                                        showTopIndicator = v
                                    },
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .semantics {
                                            contentDescription = if (showTopIndicator) {
                                                "Show top indicator"
                                            } else {
                                                "Hide top indicator"
                                            }
                                        }
                                )
                            }
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
                        Text(
                            stringResource(R.string.cancel),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    HorizontalDivider(
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
                        Text(
                            stringResource(id = R.string.confirm),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    )
}
