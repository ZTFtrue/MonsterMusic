package com.ztftrue.music.ui.other

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.play.ACTION_SEARCH
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
        val bundle = Bundle().apply { putString("keyword", keyword) }
        return suspendCancellableCoroutine { continuation ->
            musicViewModel.mediaBrowser?.sendCustomAction(
                ACTION_SEARCH,
                bundle,
                object : MediaBrowserCompat.CustomActionCallback() {
                    override fun onResult(
                        action: String?,
                        extras: Bundle?,
                        resultData: Bundle?
                    ) {
                        if (action == ACTION_SEARCH && resultData != null) {
                            val tracks = resultData.getParcelableArrayList<MusicItem>("tracks")
                                ?: emptyList()
                            val albums = resultData.getParcelableArrayList<AlbumList>("albums")
                                ?: emptyList()
                            val artists = resultData.getParcelableArrayList<ArtistList>("artist")
                                ?: emptyList()
                            continuation.resume(SearchResults(tracks, albums, artists))
                        } else {
                            // If action doesn't match or resultData is null, return empty results
                            continuation.resume(
                                SearchResults(
                                    emptyList(),
                                    emptyList(),
                                    emptyList()
                                )
                            )
                        }
                    }


                    // No need to override onProgressUpdate if not handled
                }
            ) ?: run {
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