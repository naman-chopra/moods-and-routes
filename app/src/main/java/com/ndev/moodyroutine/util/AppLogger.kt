package com.ndev.moodyroutine.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    fun toFileString(): String {
        val cleanMsg = message.replace("\n", "\\n")
        return "$timestamp\t$level\t$tag\t$cleanMsg"
    }

    companion object {
        fun fromFileString(line: String): LogEntry? {
            val parts = line.split("\t")
            if (parts.size < 4) return null
            val ts = parts[0].toLongOrNull() ?: return null
            val lvl = parts[1]
            val tg = parts[2]
            val msg = parts.subList(3, parts.size).joinToString("\t").replace("\\n", "\n")
            return LogEntry(timestamp = ts, level = lvl, tag = tg, message = msg)
        }
    }
}

object AppLogger {
    const val MAX_ENTRIES = 2500
    const val TTL_MILLIS = 48 * 60 * 60 * 1000L // 48-hour retention
    private const val MAX_LOG_FILE_BYTES = 3 * 1024 * 1024L // 3 MB max disk size

    private val logList = mutableListOf<LogEntry>()
    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    private var isLoggingEnabled = false
    private var logFile: File? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun init(context: Context) {
        val appContext = context.applicationContext
        isLoggingEnabled = PreferencesManager.isDebugLogsEnabled(appContext)
        logFile = File(appContext.filesDir, "diagnostic_logs.txt")

        loadPersistedLogs()
        i("AppLogger", "AppLogger initialized (capacity=$MAX_ENTRIES, ttl=48h, verboseDebug=$isLoggingEnabled)")
    }

    fun setLoggingEnabled(enabled: Boolean) {
        isLoggingEnabled = enabled
        i("AppLogger", "Verbose debug logging set to: $enabled")
    }

    fun isLoggingEnabled(): Boolean = isLoggingEnabled

    @Synchronized
    private fun loadPersistedLogs() {
        val file = logFile ?: return
        if (!file.exists()) return

        try {
            val now = System.currentTimeMillis()
            val cutoff = now - TTL_MILLIS
            val loaded = mutableListOf<LogEntry>()

            file.forEachLine { line ->
                if (line.isNotBlank()) {
                    LogEntry.fromFileString(line)?.let { entry ->
                        if (entry.timestamp >= cutoff) {
                            loaded.add(entry)
                        }
                    }
                }
            }

            logList.clear()
            if (loaded.size > MAX_ENTRIES) {
                logList.addAll(loaded.subList(loaded.size - MAX_ENTRIES, loaded.size))
            } else {
                logList.addAll(loaded)
            }
            _logsFlow.value = logList.toList()

            // If some lines were pruned by TTL or max capacity, rewrite cleanly
            if (loaded.size != logList.size) {
                rewriteLogFile()
            }
        } catch (e: Exception) {
            Log.e("AppLogger", "Error loading persisted diagnostic logs", e)
        }
    }

    @Synchronized
    private fun rewriteLogFile() {
        val file = logFile ?: return
        ioScope.launch {
            try {
                val snapshot = synchronized(this@AppLogger) { logList.toList() }
                file.bufferedWriter().use { writer ->
                    for (entry in snapshot) {
                        writer.write(entry.toFileString())
                        writer.newLine()
                    }
                }
            } catch (e: Exception) {
                Log.e("AppLogger", "Error rewriting diagnostic log file", e)
            }
        }
    }

    @Synchronized
    private fun log(level: String, tag: String, message: String, tr: Throwable? = null) {
        val fullMessage = if (tr != null) "$message\n${Log.getStackTraceString(tr)}" else message
        when (level) {
            "D" -> Log.d(tag, fullMessage)
            "I" -> Log.i(tag, fullMessage)
            "W" -> Log.w(tag, fullMessage)
            "E" -> Log.e(tag, fullMessage)
        }

        // INFO, WARN, and ERROR are always collected.
        // DEBUG is collected only when verbose debug logging is enabled.
        val shouldSave = isLoggingEnabled || level == "I" || level == "W" || level == "E"
        if (shouldSave) {
            val entry = LogEntry(level = level, tag = tag, message = fullMessage)
            logList.add(entry)

            val now = System.currentTimeMillis()
            val cutoff = now - TTL_MILLIS

            // Prune if exceeding capacity or older than TTL
            while (logList.isNotEmpty() && (logList.size > MAX_ENTRIES || logList.first().timestamp < cutoff)) {
                logList.removeAt(0)
            }
            _logsFlow.value = logList.toList()

            // Append to rolling disk file
            val file = logFile
            if (file != null) {
                ioScope.launch {
                    try {
                        if (file.length() > MAX_LOG_FILE_BYTES) {
                            rewriteLogFile()
                        } else {
                            file.appendText(entry.toFileString() + "\n")
                        }
                    } catch (_: Exception) {}
                }
            }
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
        val file = logFile
        if (file != null && file.exists()) {
            ioScope.launch {
                try {
                    file.delete()
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun exportLogsToUri(context: Context, uri: android.net.Uri): Result<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val content = getFormattedLogs()
            context.contentResolver.openOutputStream(uri, "wt")?.use { os ->
                os.write(content.toByteArray(Charsets.UTF_8))
                os.flush()
            } ?: return@withContext Result.failure(Exception("Could not open output stream for export"))
            
            i("AppLogger", "Successfully exported logs to selected directory")
            Result.success("Exported logs successfully")
        } catch (e: Exception) {
            e("AppLogger", "Error exporting logs to storage", e)
            Result.failure(e)
        }
    }

    fun exportLogsToFile(context: Context): File {
        val file = File(context.cacheDir, "moodyroutine_debug_logs.txt")
        file.writeText(getFormattedLogs())
        return file
    }
}

