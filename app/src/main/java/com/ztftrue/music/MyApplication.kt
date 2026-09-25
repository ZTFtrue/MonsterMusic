package com.ztftrue.music

import android.app.Application
import android.content.Intent
import android.content.ServiceConnection
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter


class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!BuildConfig.DEBUG) {
            Thread.setDefaultUncaughtExceptionHandler { thread, e ->
                if (isHarmlessServiceUnbindException(e)) {
                    Log.w("MyApplication", "Ignoring harmless service unbind exception on ${thread.name}: ${e.message}")
                    return@setDefaultUncaughtExceptionHandler
                }
                handleUncaughtException(e)
            }
        }
    }

    override fun unbindService(conn: ServiceConnection) {
        try {
            super.unbindService(conn)
        } catch (e: IllegalArgumentException) {
            Log.w("MyApplication", "ServiceConnection already unbound or not registered: ${e.message}")
        } catch (e: Exception) {
            Log.e("MyApplication", "Unexpected exception during unbindService", e)
        }
    }

    private fun isHarmlessServiceUnbindException(e: Throwable): Boolean {
        var current: Throwable? = e
        while (current != null) {
            val msg = current.message ?: ""
            if (current is IllegalArgumentException && msg.contains("Service not registered")) {
                return true
            }
            current = current.cause
        }
        return false
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
        android.os.Process.killProcess(android.os.Process.myPid())
        kotlin.system.exitProcess(10)
    }
}