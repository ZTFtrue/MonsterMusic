package com.ztftrue.music

import android.app.Application
import android.content.Intent


class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            handleUncaughtException(thread, e)
        }
    }

    private fun handleUncaughtException(thread: Thread, e: Throwable) {
        val intent = Intent(applicationContext, ErrorTipActivity::class.java)
        intent.putExtra("error", e)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent)
    }
}