package com.ztftrue.music

import android.app.Application
import android.content.Intent


class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
//        Thread.setDefaultUncaughtExceptionHandler { _, e ->
//            handleUncaughtException(e)
//        }
    }

    private fun handleUncaughtException(e: Throwable) {

        val intent = Intent(applicationContext, ErrorTipActivity::class.java)
        intent.putExtra("error", e)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}