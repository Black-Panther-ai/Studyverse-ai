package com.example

import android.app.Application
import android.content.Context
import android.util.Log

class StudySwapApplication : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        setupGlobalExceptionHandler(base)
    }

    private fun setupGlobalExceptionHandler(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("StudySwapCrash", "Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
                saveCrashReport(context, throwable, thread.name)
            } catch (e: Exception) {
                // Prevent handler itself from throwing
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashReport(context: Context, throwable: Throwable, threadName: String) {
        try {
            val sharedPrefs = context.getSharedPreferences("startup_crash_prefs", Context.MODE_PRIVATE)
            val sw = java.io.StringWriter()
            throwable.printStackTrace(java.io.PrintWriter(sw))
            val report = """
                Timestamp: ${System.currentTimeMillis()}
                Exception: ${throwable.javaClass.name}
                Message: ${throwable.message}
                Thread: $threadName
                Android SDK: ${android.os.Build.VERSION.SDK_INT}
                Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
                StackTrace:
                $sw
            """.trimIndent()
            sharedPrefs.edit().putString("last_crash_report", report).commit()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
