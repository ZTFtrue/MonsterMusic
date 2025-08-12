package com.ztftrue.music.ui.other

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.ui.home.AlbumGridView
import com.ztftrue.music.ui.home.ArtistsGridView
import com.ztftrue.music.ui.public.BackButton
import com.ztftrue.music.ui.public.Bottom
import com.ztftrue.music.ui.public.TracksListView
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.ScrollDirectionType
import com.ztftrue.music.utils.model.AnyListBase


/**
 * Composable function for the Search screen.
 * Handles user input for search, displays search results (tracks, albums, artists).
 */
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPage(
    musicViewModel: MusicViewModel,
    navController: NavHostController,
    // Inject SearchScreenViewModel using viewModel() helper with a factory
    searchScreenViewModel: SearchScreenViewModel = viewModel(
        factory = SearchScreenViewModelFactory(musicViewModel)
    )
) {
    // Collect state from the ViewModel
    val keywords by searchScreenViewModel.keywords.collectAsState()
//    val tracksList by searchScreenViewModel.tracksList.collectAsState()
//    val albumsList by searchScreenViewModel.albumsList.collectAsState()
//    val artistList by searchScreenViewModel.artistList.collectAsState()
    var modeList by remember { mutableStateOf(AnyListBase(-1, PlayListType.None)) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val windowInfo = LocalWindowInfo.current
    val containerWidth = windowInfo.containerSize.width
    // Calculate item width for horizontal scrollable grids (albums, artists)
    val width = (containerWidth / 2.5) + 70
    val rootView = LocalView.current
    val localViewHeight by remember { mutableIntStateOf(rootView.height) }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(navController) },
                title = { /* Empty title, search bar is in actions */ },
                actions = {
                    val density = LocalDensity.current
                    val containerWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
                    Row(
                        modifier = Modifier
                            .width(containerWidthDp - 60.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Using OutlinedTextField for a distinct search bar look
                        OutlinedTextField(
                            value = keywords,
                            onValueChange = { searchScreenViewModel.onKeywordsChange(it) },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.enter_text_to_search),
                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.38f
                                    )
                                )
                            },
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Search, // Keyboard action for search
                                keyboardType = KeyboardType.Text
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    // Hide keyboard and clear focus when search action is performed
                                    keyboardController?.hide()
                                    focusRequester.freeFocus()
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
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
                            singleLine = true // Search fields are typically single-line
                        )
                    }
                }
            )
        },
        bottomBar = { Bottom(musicViewModel, navController) },
        content = { paddingValues ->
            // Determine if "No music" message should be shown
            val hasResults =
                searchScreenViewModel._albumsList.isNotEmpty()
                        || searchScreenViewModel._artistList.isNotEmpty()
                        || searchScreenViewModel._tracksList.isNotEmpty()
            // Show message if keywords are not empty, longer than 1 character, and no results are found
            val showNoMusicMessage = keywords.isNotEmpty() && !hasResults && keywords.length > 1

            if (showNoMusicMessage) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp), // Add some horizontal padding for text
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_music),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            } else if (hasResults) {
                var height = localViewHeight
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    TracksListView(
                        musicViewModel,
                        modeList, searchScreenViewModel._tracksList, remember {
                            mutableStateOf(true)
                        }
                    ) {
                        if ( searchScreenViewModel._albumsList.isNotEmpty()) {
                            height -= width.toInt()
                            Text(
                                text = stringResource(R.string.album, ""),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Box(
                                modifier = Modifier
                                    .height(width.dp)
                                    .background(MaterialTheme.colorScheme.secondary)
                                    .fillMaxWidth()
                            ) {
                                AlbumGridView(
                                    musicViewModel = musicViewModel,
                                    navController = navController,
                                    albumListDefault =  searchScreenViewModel._albumsList,
                                    scrollDirection = ScrollDirectionType.GRID_HORIZONTAL
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(color = MaterialTheme.colorScheme.onBackground)
                            )
                        }
                        if (searchScreenViewModel._artistList.isNotEmpty()) {
                            height -= width.toInt()
                            Text(
                                text = stringResource(R.string.artist, ""),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Box(
                                modifier = Modifier
                                    .height(width.dp)
                                    .background(MaterialTheme.colorScheme.secondary)
                                    .fillMaxWidth()
                            ) {
                                ArtistsGridView(
                                    musicViewModel = musicViewModel,
                                    navController = navController,
                                    artistListDefault =  searchScreenViewModel._artistList,
                                    scrollDirection = ScrollDirectionType.GRID_HORIZONTAL
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(color = MaterialTheme.colorScheme.onBackground)
                            )
                        }
                    }
                }
            }
        },
    )
}
