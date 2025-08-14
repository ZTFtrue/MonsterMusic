package com.ztftrue.music.ui.other

import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.play.MediaCommands
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.model.AlbumList
import com.ztftrue.music.utils.model.ArtistList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Data class to hold search results
 */
data class SearchResults(
    val tracks: List<MusicItem>,
    val albums: List<AlbumList>,
    val artists: List<ArtistList>
)

/**
 * ViewModel for the Search screen to handle search logic and state.
 * Uses Flow for debouncing and managing search queries.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchScreenViewModel(
    private val musicViewModel: MusicViewModel // Injected App-level MusicViewModel
) : ViewModel() {

    private val _keywords = MutableStateFlow("")
    val keywords: StateFlow<String> = _keywords.asStateFlow()

    val _tracksList = mutableStateListOf<MusicItem>()

    val _albumsList = mutableStateListOf<AlbumList>()

    val _artistList = mutableStateListOf<ArtistList>()


    init {
        viewModelScope.launch {
            _keywords
                .debounce(300L) // Debounce user input to avoid excessive search calls
                .filter { it.length > 1 || it.isEmpty() } // Only search for keywords longer than 1 char, or when cleared
                .mapLatest { keyword -> // Use mapLatest to cancel previous search if new keyword arrives
                    if (keyword.length > 1) {
                        performSearch(keyword)
                    } else {
                        // Clear results if keyword is too short or empty
                        SearchResults(emptyList(), emptyList(), emptyList())
                    }
                }
                .collect { searchResults ->
                    _tracksList.clear()
                    _tracksList.addAll(searchResults.tracks)
                    _albumsList.clear()
                    _albumsList.addAll(searchResults.albums)
                    _artistList.clear()
                    _artistList.addAll(searchResults.artists)
                }
        }
    }

    /**
     * Updates the search keywords.
     * @param newKeywords The new keyword string.
     */
    fun onKeywordsChange(newKeywords: String) {
        _keywords.value = newKeywords
        // Immediately clear results if keywords become empty, without debouncing
        if (newKeywords.isEmpty()) {
            _tracksList.clear()
            _albumsList.clear()
            _artistList.clear()
        }
    }

    /**
     * Performs the actual search via MediaBrowser.
     * Uses suspendCancellableCoroutine to bridge Flow with MediaBrowser's callback API.
     * @param keyword The keyword to search for.
     * @return SearchResults containing lists of tracks, albums, and artists.
     */
    private suspend fun performSearch(keyword: String): SearchResults {
        val bundle = Bundle().apply { putString(MediaCommands.KEY_SEARCH_QUERY, keyword) }
        return suspendCancellableCoroutine { continuation ->

            val futureResult: ListenableFuture<SessionResult>? =
                musicViewModel.browser?.sendCustomCommand(
                    MediaCommands.COMMAND_SEARCH,
                    bundle
                )
            futureResult?.addListener({
                try {
                    // a. 获取 SessionResult
                    val sessionResult = futureResult.get()
                    // b. 检查操作是否成功
                    if (sessionResult.resultCode == SessionResult.RESULT_SUCCESS) {
                        val tracks =
                            sessionResult.extras.getParcelableArrayList<MusicItem>("tracks")
                                ?: emptyList()
                        val albums =
                            sessionResult.extras.getParcelableArrayList<AlbumList>("albums")
                                ?: emptyList()
                        val artists =
                            sessionResult.extras.getParcelableArrayList<ArtistList>("artist")
                                ?: emptyList()
                        continuation.resume(SearchResults(tracks, albums, artists))
                    } else {
                        continuation.resume(
                            SearchResults(
                                emptyList(),
                                emptyList(),
                                emptyList()
                            )
                        )
                    }
                } catch (e: Exception) {
                    // 处理在获取结果过程中可能发生的异常 (如 ExecutionException)
                    Log.e("Client", "Failed to toggle favorite status", e)
                }
            }, MoreExecutors.directExecutor()) ?: run {
                // If mediaBrowser is null, resume with empty results immediately
                continuation.resume(SearchResults(emptyList(), emptyList(), emptyList()))
            }
        }
    }
}

/**
 * Factory for creating SearchScreenViewModel instances,
 * required when a ViewModel has constructor parameters (like MusicViewModel).
 */
class SearchScreenViewModelFactory(private val musicViewModel: MusicViewModel) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchScreenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchScreenViewModel(musicViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}