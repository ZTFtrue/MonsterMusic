package com.ztftrue.music.ui.other

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.ztftrue.music.MainActivity
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.Router
import com.ztftrue.music.utils.Utils
import kotlinx.coroutines.launch

@UnstableApi
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawMenu(
    pagerState: PagerState,
    drawerState: DrawerState,
    navController: NavHostController,
    musicViewModel: MusicViewModel,
    activity: MainActivity
) {
    val drawerWidth = 240.dp
    val scope = rememberCoroutineScope()
    val color = MaterialTheme.colorScheme.onBackground
    ModalDrawerSheet(
        modifier = Modifier
            .width(drawerWidth)
            .background(MaterialTheme.colorScheme.onBackground),
    ) {
        Column(
            Modifier
                .width(drawerWidth)
                .fillMaxHeight()
                .padding(all = 0.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            ConstraintLayout(
                modifier = Modifier
                    .width(drawerWidth)
                    .height(180.dp)
            ) {
                val (bgImage, header) = createRefs()
                Image(
                    painter = painterResource(
                        id = R.drawable.large_cover
                    ),
                    contentDescription = "Album cover",
                    modifier = Modifier
                        .width(drawerWidth)
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.onBackground)
                        .constrainAs(bgImage) {
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        },
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.BottomStart
                )
                Image(
                    painter = rememberAsyncImagePainter(
                        musicViewModel.getCurrentMusicCover()
                            ?: R.drawable.songs_thumbnail_cover
                    ),
                    contentDescription = "Album cover",
                    modifier = Modifier
                        .width(80.dp)
                        .height(80.dp)
                        .clip(CircleShape)
                        .constrainAs(header) {
                            top.linkTo(parent.top, margin = 30.dp)
                            start.linkTo(parent.start, margin = 30.dp)
                        },
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center
                )
            }


//                    musicViewModel.mainTabList.forEachIndexed() { i, it ->
//                        Box(modifier = Modifier.clickable {
//                            scope.launch {
//                                pagerState.scrollToPage(i)
//                                drawerState.close()
//                            }
//                        }) {
//                            Text(text = it.name)
//                        }
//                    }
            Box(
                modifier = Modifier
                    .width(drawerWidth)
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
                        scope.launch {
                            drawerState.close()
                        }
                        navController.navigate(
                            Router.SettingsPage.withArgs()
                        )
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(R.string.settings),
                    Modifier.padding(start = 10.dp),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            var showFeedBackDialog by remember { mutableStateOf(false) }
            if (showFeedBackDialog) {
                FeedBackDialog {
                    showFeedBackDialog = false
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .drawBehind {
                        drawLine(
                            color = color,
                            start = Offset(0f, size.height - 1.dp.toPx()),
                            end = Offset(size.width, size.height - 1.dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .clickable {
                        scope.launch {
                            drawerState.close()
                        }
                        showFeedBackDialog = true
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(R.string.feedback),
                    Modifier.padding(start = 10.dp),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .drawBehind {
                        drawLine(
                            color = color,
                            start = Offset(0f, size.height - 1.dp.toPx()),
                            end = Offset(size.width, size.height - 1.dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .clickable {

                        musicViewModel.mediaBrowser?.sendCustomAction(
                            "com.ztftrue.music.ACTION_EXIT",
                            null,
                            null
                        )
                        activity.finish()
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(R.string.exit_app), Modifier.padding(start = 10.dp),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@Composable
fun FeedBackDialog(onDismiss: () -> Unit) {

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
                    text = stringResource(R.string.about_thanks), modifier = Modifier
                        .padding(2.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color = MaterialTheme.colorScheme.onSurface)
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
                                    onConfirmation()
                                    Utils.openBrowser(
                                        "https://github.com/ZTFtrue/MonsterMusic/issues",
                                        context
                                    )
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = stringResource(R.string.to_github),
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
                                    onConfirmation()
                                    Utils.sendEmail(
                                        "ztftrue@gmail.com",
                                        "MonsterMusic FeedBack",
                                        context
                                    )
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = stringResource(R.string.send_email), Modifier.padding(start = 10.dp),
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
                        Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }
    )
}
