package com.ztftrue.music

import android.app.Application
import android.content.Intent
import java.io.PrintWriter
import java.io.StringWriter




class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
//        if(BuildConfig.DEBUG) {
//            Thread.setDefaultUncaughtExceptionHandler { _, e ->
//                handleUncaughtException(e)
//            }
//        }
    }

    private fun handleUncaughtException(e: Throwable) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        e.printStackTrace(pw)
        val sStackTrace = sw.toString() // stack trace as a string
        val intent = Intent(applicationContext, ErrorTipActivity::class.java)
        intent.putExtra("error", sStackTrace)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}