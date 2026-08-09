package com.example.util

import android.util.Log

object ProductionDiagnostics {
    private const val TAG = "StudyverseDiagnostics"

    private val isCrashlyticsAvailable: Boolean by lazy {
        try {
            Class.forName("com.google.firebase.crashlytics.FirebaseCrashlytics")
            true
        } catch (e: Exception) {
            false
        }
    }

    fun log(message: String) {
        Log.d(TAG, message)
        if (isCrashlyticsAvailable) {
            try {
                val crashlyticsClass = Class.forName("com.google.firebase.crashlytics.FirebaseCrashlytics")
                val getInstanceMethod = crashlyticsClass.getMethod("getInstance")
                val instance = getInstanceMethod.invoke(null)
                val logMethod = crashlyticsClass.getMethod("log", String::class.java)
                logMethod.invoke(instance, message)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun logError(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.e(TAG, message)
        }
        if (isCrashlyticsAvailable && throwable != null) {
            try {
                val crashlyticsClass = Class.forName("com.google.firebase.crashlytics.FirebaseCrashlytics")
                val getInstanceMethod = crashlyticsClass.getMethod("getInstance")
                val instance = getInstanceMethod.invoke(null)
                val recordExceptionMethod = crashlyticsClass.getMethod("recordException", Throwable::class.java)
                recordExceptionMethod.invoke(instance, throwable)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun setCustomKey(key: String, value: String) {
        Log.d(TAG, "Diagnostic Key: $key = $value")
        if (isCrashlyticsAvailable) {
            try {
                val crashlyticsClass = Class.forName("com.google.firebase.crashlytics.FirebaseCrashlytics")
                val getInstanceMethod = crashlyticsClass.getMethod("getInstance")
                val instance = getInstanceMethod.invoke(null)
                val setCustomKeyMethod = crashlyticsClass.getMethod("setCustomKey", String::class.java, String::class.java)
                setCustomKeyMethod.invoke(instance, key, value)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun setCustomKey(key: String, value: Boolean) {
        Log.d(TAG, "Diagnostic Key: $key = $value")
        if (isCrashlyticsAvailable) {
            try {
                val crashlyticsClass = Class.forName("com.google.firebase.crashlytics.FirebaseCrashlytics")
                val getInstanceMethod = crashlyticsClass.getMethod("getInstance")
                val instance = getInstanceMethod.invoke(null)
                val setCustomKeyMethod = crashlyticsClass.getMethod("setCustomKey", String::class.java, Boolean::class.javaPrimitiveType)
                setCustomKeyMethod.invoke(instance, key, value)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun setCustomKey(key: String, value: Long) {
        Log.d(TAG, "Diagnostic Key: $key = $value")
        if (isCrashlyticsAvailable) {
            try {
                val crashlyticsClass = Class.forName("com.google.firebase.crashlytics.FirebaseCrashlytics")
                val getInstanceMethod = crashlyticsClass.getMethod("getInstance")
                val instance = getInstanceMethod.invoke(null)
                val setCustomKeyMethod = crashlyticsClass.getMethod("setCustomKey", String::class.java, Long::class.javaPrimitiveType)
                setCustomKeyMethod.invoke(instance, key, value)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
