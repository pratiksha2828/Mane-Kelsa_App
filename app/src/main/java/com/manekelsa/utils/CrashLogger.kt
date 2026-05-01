package com.manekelsa.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {
    private const val TAG = "CrashLogger"
    private const val FILE_NAME = "crash_log.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val header = "\n--- Crash $timestamp ---\n"
                val message = header + Log.getStackTraceString(throwable) + "\n"
                Log.e(TAG, message)

                val file = File(appContext.filesDir, FILE_NAME)
                file.appendText(message)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write crash log", e)
            } finally {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
