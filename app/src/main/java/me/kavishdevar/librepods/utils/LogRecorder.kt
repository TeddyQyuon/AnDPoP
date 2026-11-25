// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.utils

import android.content.Context
import android.os.Build
import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.kavishdevar.librepods.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Records AnDPoP's own log while the user reproduces a problem. Android lets an app read its own
 * log entries without root, so this works on every phone. Nothing leaves the phone unless the
 * user shares a saved file. Bluetooth addresses are shortened before the file is written.
 */
class LogRecorder(private val context: Context) {
    private var process: java.lang.Process? = null
    private var reader: Job? = null
    private val lines = StringBuilder()

    val isRecording: Boolean get() = process != null

    /** Starts capturing log lines written from now on by this app's process. */
    fun start(scope: CoroutineScope) {
        if (process != null) return
        synchronized(lines) { lines.setLength(0) }
        val since = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val started = runCatching {
            Runtime.getRuntime().exec(arrayOf("logcat", "-v", "threadtime", "--pid=${Process.myPid()}", "-T", since))
        }.getOrNull() ?: return
        process = started
        reader = scope.launch(Dispatchers.IO) {
            started.inputStream.bufferedReader().use { input ->
                while (isActive) {
                    val line = input.readLine() ?: break
                    synchronized(lines) {
                        if (lines.length < MAX_CHARS) lines.append(redact(line)).append('\n')
                    }
                }
            }
        }
    }

    /** Stops capturing and saves the log. Returns the file, or null when nothing was captured. */
    suspend fun stopAndSave(deviceModel: String?): File? = withContext(Dispatchers.IO) {
        process?.destroy()
        process = null
        reader?.cancel()
        reader = null
        val body = synchronized(lines) { lines.toString() }
        if (body.isBlank()) return@withContext null
        val dir = logsDirectory(context).apply { mkdirs() }
        val name = "andpop-log-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".txt"
        val header = buildString {
            append("AnDPoP ").append(BuildConfig.VERSION_NAME).append(" (").append(BuildConfig.VERSION_CODE).append(", ")
                .append(BuildConfig.FLAVOR).append(")\n")
            append("Phone: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                .append(", Android ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
            if (!deviceModel.isNullOrBlank()) append("AirPods: ").append(deviceModel).append('\n')
            append("Bluetooth addresses are shortened to their last two bytes.\n\n")
        }
        File(dir, name).apply { writeText(header + body) }
    }

    companion object {
        private const val MAX_CHARS = 4 * 1024 * 1024
        private val MAC = Regex("\\b(?:[0-9A-Fa-f]{2}[:-]){4}([0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2})\\b")

        fun logsDirectory(context: Context) = File(context.filesDir, "logs")

        /** Keeps only the last two bytes of each Bluetooth address, for example XX:XX:XX:XX:AB:CD. */
        fun redact(line: String): String = MAC.replace(line) { "XX:XX:XX:XX:" + it.groupValues[1].replace('-', ':') }

        fun savedLogs(context: Context): List<File> =
            logsDirectory(context).listFiles()?.filter { it.isFile && it.name.endsWith(".txt") }
                ?.sortedByDescending { it.lastModified() }.orEmpty()
    }
}
