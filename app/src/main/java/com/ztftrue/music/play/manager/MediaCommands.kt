package com.ztftrue.music.play.manager

import android.os.Bundle
import androidx.media3.session.SessionCommand

object MediaCommands {
    // DSP & Effects
    val COMMAND_CHANGE_PITCH = SessionCommand("dsp.CHANGE_PITCH", Bundle.EMPTY)
    val COMMAND_CHANGE_Q = SessionCommand("dsp.CHANGE_Q", Bundle.EMPTY)
    val COMMAND_DSP_ENABLE = SessionCommand("dsp.ENABLE", Bundle.EMPTY)
    val COMMAND_VIRTUALIZER_ENABLE = SessionCommand("dsp.VIRTUALIZER_ENABLE", Bundle.EMPTY)
    val COMMAND_VIRTUALIZER_STRENGTH = SessionCommand("dsp.VIRTUALIZER_STRENGTH", Bundle.EMPTY)

    // DSP
    val COMMAND_DSP_SET_BAND = SessionCommand("dsp.SET_BAND", Bundle.EMPTY)
    val COMMAND_DSP_FLATTEN = SessionCommand("dsp.FLATTEN", Bundle.EMPTY)
    val COMMAND_DSP_SET_BANDS = SessionCommand("dsp.SET_BANDS", Bundle.EMPTY)

    // Echo
    val COMMAND_ECHO_ENABLE = SessionCommand("echo.ENABLE", Bundle.EMPTY)
    val COMMAND_ECHO_SET_DELAY = SessionCommand("echo.SET_DELAY", Bundle.EMPTY)
    val COMMAND_ECHO_SET_DECAY = SessionCommand("echo.SET_DECAY", Bundle.EMPTY)
    val COMMAND_ECHO_SET_FEEDBACK = SessionCommand("echo.SET_FEEDBACK", Bundle.EMPTY)

    val COMMAND_SEARCH = SessionCommand("app.SEARCH", Bundle.EMPTY)

    val COMMAND_SET_AUTO_HANDLE_AUDIO_FOCUS =
        SessionCommand("app.SET_AUTO_HANDLE_AUDIO_FOCUS", Bundle.EMPTY)


    // Visualization
    val COMMAND_VISUALIZATION_ENABLE = SessionCommand("vis.ENABLE", Bundle.EMPTY)

    // Sleep Timer
    val COMMAND_SET_SLEEP_TIMER = SessionCommand("timer.SET_SLEEP", Bundle.EMPTY)
    val COMMAND_SLEEP_STATE_UPDATE = SessionCommand("timer.SLEEP_STATE", Bundle.EMPTY)

    val COMMAND_VISUALIZATION_CONNECTED = SessionCommand("vis.CONNECTED", Bundle.EMPTY)
    val COMMAND_GET_INITIALIZED_DATA = SessionCommand("vis.GET_INITIALIZED_DATA", Bundle.EMPTY)

    // 命令：通知 Service 可视化组件将要断开/不可见
    val COMMAND_VISUALIZATION_DISCONNECTED = SessionCommand("vis.DISCONNECTED", Bundle.EMPTY)

    val COMMAND_APP_EXIT = SessionCommand("app.EXIT", Bundle.EMPTY)
    val COMMAND_TRACKS_UPDATE = SessionCommand("tracks.UPDATE", Bundle.EMPTY)
    val COMMAND_TRACK_DELETE = SessionCommand("app.TRACK_DELETE", Bundle.EMPTY)
    val COMMAND_PlAY_LIST_CHANGE = SessionCommand("app.PlAY_LIST_CHANGE", Bundle.EMPTY)
    val COMMAND_GET_PLAY_LIST_ITEM = SessionCommand("app.GET_PLAY_LIST_ITEM", Bundle.EMPTY)

    val COMMAND_SORT_QUEUE = SessionCommand("queue.SORT_QUEUE", Bundle.EMPTY)
    val COMMAND_SORT_TRACKS = SessionCommand("queue.SORT_TRACKS", Bundle.EMPTY)

    val COMMAND_CHANGE_PLAYLIST = SessionCommand("queue.CHANGE_PLAYLIST", Bundle.EMPTY)
    val COMMAND_GET_CURRENT_PLAYLIST =
        SessionCommand("queue.GET_CURRENT_PLAYLIST", Bundle.EMPTY)
    val COMMAND_SMART_SHUFFLE = SessionCommand("queue.SMART_SHUFFLE", Bundle.EMPTY)

    // Key for the new playlist

    // Key for the item to start playing from
    const val KEY_START_MEDIA_ID = "key_start_media_id"


    // --- 定义所有 Bundle Key ---
    const val KEY_INDEX = "index"
    const val KEY_VALUE = "value"
    const val KEY_TRACK_ID = "long_id_value"
    const val KEY_ENABLE = "enable"
    const val KEY_DELAY = "delay"
    const val KEY_DECAY = "decay"
    const val KEY_SEARCH_QUERY = "search_query"

    val COMMAND_CLEAR_QUEUE = SessionCommand("queue.CLEAR", Bundle.EMPTY)

    val COMMAND_REFRESH_ALL = SessionCommand("app.REFRESH_ALL", Bundle.EMPTY)

}