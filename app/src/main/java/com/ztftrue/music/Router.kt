package com.ztftrue.music

sealed class Router(val route: String) {
    data object MainView : Router(route = "MainView")
    data object MusicPlayerView : Router(route = "MusicPlayerView")
    data object PlayListView : Router(route = "PlayListView")
    data object SettingsPage : Router(route = "SettingsPage")
    data object QueuePage : Router(route = "QueuePage")
    data object TracksSelectPage : Router(route = "TracksSelectPage")
    data object SearchPage : Router(route = "SearchPage")
    data object EditTrackPage : Router(route = "EditTrackPage")

    fun withArgs(vararg args: String): String {
        return buildString {
            append(route)
            args.forEach { arg ->
                append("/$arg")
            }
        }
    }
}