package com.ndev.moodyroutine.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: String, // D, I, W, E
    val tag: String,
    val message: String
) {
    fun formatted(): String {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
        return "$time [$level/$tag] $message"
    }
}

object AppLogger {
    private const val MAX_ENTRIES = 500
    private val logList = mutableListOf<LogEntry>()
    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    private var isLoggingEnabled = false

    fun init(context: Context) {
        isLoggingEnabled = PreferencesManager.isDebugLogsEnabled(context)
        i("AppLogger", "AppLogger initialized. Debug logs enabled = $isLoggingEnabled")
    }

    fun setLoggingEnabled(enabled: Boolean) {
        isLoggingEnabled = enabled
        i("AppLogger", "Debug logging set to: $enabled")
    }

    fun isLoggingEnabled(): Boolean = isLoggingEnabled

    @Synchronized
    private fun log(level: String, tag: String, message: String, tr: Throwable? = null) {
        val fullMessage = if (tr != null) "$message\n${Log.getStackTraceString(tr)}" else message
        when (level) {
            "D" -> Log.d(tag, fullMessage)
            "I" -> Log.i(tag, fullMessage)
            "W" -> Log.w(tag, fullMessage)
            "E" -> Log.e(tag, fullMessage)
        }

        if (isLoggingEnabled || level == "E" || level == "W") {
            val entry = LogEntry(level = level, tag = tag, message = fullMessage)
            logList.add(entry)
            if (logList.size > MAX_ENTRIES) {
                logList.removeAt(0)
            }
            _logsFlow.value = logList.toList()
        }
    }

    fun d(tag: String, message: String) = log("D", tag, message)
    fun i(tag: String, message: String) = log("I", tag, message)
    fun w(tag: String, message: String, tr: Throwable? = null) = log("W", tag, message, tr)
    fun e(tag: String, message: String, tr: Throwable? = null) = log("E", tag, message, tr)

    @Synchronized
    fun getFormattedLogs(): String {
        return logList.joinToString("\n") { it.formatted() }
    }

    @Synchronized
    fun clearLogs() {
        logList.clear()
        _logsFlow.value = emptyList()
    }

    fun exportLogsToFile(context: Context): File {
        val file = File(context.cacheDir, "moodyroutine_debug_logs.txt")
        file.writeText(getFormattedLogs())
        return file
    }
}
