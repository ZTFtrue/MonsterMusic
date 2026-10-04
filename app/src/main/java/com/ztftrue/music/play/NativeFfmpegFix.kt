package com.ztftrue.music.play

import android.content.Context
import android.util.Log

object NativeFfmpegFix {
    private const val TAG = "NativeFfmpegFix"
    @Volatile
    private var isInstalled = false

    @Synchronized
    @JvmOverloads
    fun installFix(context: Context? = null): Boolean {
        if (isInstalled) return true
        return try {
            System.loadLibrary("monster_audio")
            val libPath = context?.applicationInfo?.nativeLibraryDir?.let { "$it/libffmpegJNI.so" }
            val ok = installNativeHook(libPath)
            isInstalled = ok
            if (ok) {
                Log.i(TAG, "FFmpeg native decode hardening hook successfully installed")
            } else {
                Log.w(TAG, "FFmpeg native decode hook could not be installed (falling back to large buffer sizing)")
            }
            ok
        } catch (t: Throwable) {
            Log.w(TAG, "Error installing FFmpeg hook: ${t.message}")
            false
        }
    }

    @JvmStatic
    private external fun installNativeHook(libPath: String?): Boolean
}
