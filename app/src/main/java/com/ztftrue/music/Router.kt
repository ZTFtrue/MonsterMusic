package com.ztftrue.music

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Router(val route: String) {
    data object MainView : Router(route = "MainView")
    data object MusicPlayerView : Router(route = "MusicPlayerView")
    data object PlayListView : Router(route = "PlayListView")
    data object SettingsPage : Router(route = "SettingsPage")
    data object QueuePage : Router(route = "QueuePage")
    data object TracksSelectPage : Router(route = "TracksSelectPage")
    data object SearchPage : Router(route = "SearchPage")
    data object EditTrackPage : Router(route = "EditTrackPage")

    fun withArgs(vararg pairs: Pair<String, Any?>): String {
        val validArgs = pairs.filter { it.second != null }
        if (validArgs.isEmpty()) {
            return route
        }
        return buildString {
            append(route)
            validArgs.forEachIndexed { index, pair ->
                val (key, value) = pair
                val encodedValue =
                    URLEncoder.encode(value.toString(), StandardCharsets.UTF_8.name())
                append(if (index == 0) '?' else '&')
                append("$key=$encodedValue")
            }
        }
    }
}