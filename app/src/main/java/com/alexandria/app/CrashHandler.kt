package com.alexandria.app

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

object CrashHandler {

    private const val CRASH_FILE_NAME = "crash_log.txt"

    fun init(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val crashInfo = buildCrashInfo(throwable)
                writeCrashToFile(context, crashInfo)
                Log.e("CrashHandler", "Crash captured", throwable)
            } catch (e: Exception) {
                Log.e("CrashHandler", "Failed to write crash log", e)
            }

            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun getCrashLog(context: Context): String? {
        val file = File(context.filesDir, CRASH_FILE_NAME)
        return if (file.exists()) {
            val content = file.readText()
            file.delete()
            content
        } else {
            null
        }
    }

    private fun buildCrashInfo(throwable: Throwable): String {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        return buildString {
            appendLine("=== ALEXANDRIA CRASH REPORT ===")
            appendLine("Fecha: $timestamp")
            appendLine("Error: ${throwable.javaClass.name}")
            appendLine("Mensaje: ${throwable.message ?: "(sin mensaje)"}")
            appendLine()
            appendLine("=== STACK TRACE ===")
            appendLine(sw.toString())
        }
    }

    private fun writeCrashToFile(context: Context, crashInfo: String) {
        val file = File(context.filesDir, CRASH_FILE_NAME)
        file.writeText(crashInfo)
    }
}
