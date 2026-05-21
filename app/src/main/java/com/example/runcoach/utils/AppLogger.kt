package com.example.runcoach.utils

import android.util.Log

/**
 * A custom logging utility that automatically extracts the caller's class name,
 * method name, and line number to format the log tag.
 */
object AppLogger {
    private const val GLOBAL_TAG = "RunCoachLog"

    fun v(message: String) = log(Log.VERBOSE, message)
    fun d(message: String) = log(Log.DEBUG, message)
    fun i(message: String) = log(Log.INFO, message)
    fun w(message: String) = log(Log.WARN, message)
    fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            log(Log.ERROR, "$message\n${Log.getStackTraceString(throwable)}")
        } else {
            log(Log.ERROR, message)
        }
    }

    private fun log(priority: Int, message: String) {
        val stackTrace = Thread.currentThread().stackTrace
        // Index 0: dalvik.system.VMStack.getThreadStackTrace
        // Index 1: java.lang.Thread.getStackTrace
        // Index 2: com.example.runcoach.utils.AppLogger.log
        // Index 3: com.example.runcoach.utils.AppLogger.d (or v, i, w, e)
        // Index 4: The caller method
        val caller = stackTrace.getOrNull(4)
        
        val tag = if (caller != null) {
            val className = caller.className.substringAfterLast('.')
            "$GLOBAL_TAG|$className.${caller.methodName}(${caller.lineNumber})"
        } else {
            GLOBAL_TAG
        }

        Log.println(priority, tag, message)
    }
}
