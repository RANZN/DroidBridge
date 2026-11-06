package com.ranjan.adbsend.data.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

data class AdbResult(val exitCode: Int, val stdout: String, val stderr: String) {
    val isSuccess: Boolean get() = exitCode == 0

    /** Best human-readable error message adb produced. */
    val errorMessage: String
        get() {
            val lines = (stderr.ifBlank { stdout }).lines().map { it.trim() }.filter { it.isNotEmpty() }
            // adb can print a success summary before the real error, so prefer the error line.
            return lines.lastOrNull { "error:" in it } ?: lines.lastOrNull() ?: "adb exited with code $exitCode"
        }
}

class AdbNotFoundException : IllegalStateException(
    "adb was not found. Install Android platform-tools (e.g. `brew install android-platform-tools`)."
)

/** Thin wrapper that runs adb commands as child processes. */
class AdbClient(locator: AdbLocator) {
    val adbPath: String? = locator.locate()

    suspend fun run(vararg args: String): AdbResult = run(args.toList())

    suspend fun run(args: List<String>): AdbResult = withContext(Dispatchers.IO) {
        val adb = adbPath ?: throw AdbNotFoundException()
        val process = ProcessBuilder(listOf(adb) + args).start()
        try {
            coroutineScope {
                val out = async(Dispatchers.IO) { process.inputStream.bufferedReader().readText() }
                val err = async(Dispatchers.IO) { process.errorStream.bufferedReader().readText() }
                val code = runInterruptible { process.waitFor() }
                AdbResult(code, out.await(), err.await())
            }
        } finally {
            // Kills the child process when the coroutine is cancelled.
            if (process.isAlive) {
                process.destroy()
                if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly()
            }
        }
    }

    /**
     * Runs a long adb command (push/pull) and streams its progress output line by line.
     *
     * adb only prints `[ 45%] /path` progress when attached to a terminal, so the command runs
     * inside a pseudo-terminal via macOS `script`. Falls back to a plain run without progress.
     * Progress lines go to [onOutput]; other lines become the result's stdout.
     */
    suspend fun runStreaming(args: List<String>, onOutput: (String) -> Unit): AdbResult = withContext(Dispatchers.IO) {
        val adb = adbPath ?: throw AdbNotFoundException()
        val script = File(SCRIPT_PATH)
        val command = if (script.canExecute()) listOf(SCRIPT_PATH, "-q", "/dev/null", adb) + args else listOf(adb) + args
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .redirectInput(File("/dev/null"))
            .apply { environment()["TERM"] = "xterm" }
            .start()
        try {
            val lines = mutableListOf<String>()
            runInterruptible {
                val reader = process.inputStream.bufferedReader()
                val buffer = StringBuilder()
                fun flush() {
                    val line = cleanTerminalOutput(buffer.toString())
                    buffer.clear()
                    if (line.isBlank()) return
                    if (AdbParsers.parseProgress(line) != null) onOutput(line) else lines += line
                }
                while (true) {
                    val c = reader.read()
                    if (c == -1) break
                    if (c == '\r'.code || c == '\n'.code) flush() else buffer.append(c.toChar())
                }
                flush()
            }
            val code = runInterruptible { process.waitFor() }
            AdbResult(code, lines.joinToString("\n"), "")
        } finally {
            if (process.isAlive) {
                process.destroy()
                if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly()
            }
        }
    }

    /** Runs a command inside `adb shell` on [serial]. */
    suspend fun shell(serial: String, command: String): AdbResult =
        run("-s", serial, "shell", command)
}

private const val SCRIPT_PATH = "/usr/bin/script"

private val ansiEscape = Regex("\u001B\\[[0-9;?]*[A-Za-z]")

/** Strips terminal escape codes and control characters (e.g. ^D, backspace) from pty output. */
internal fun cleanTerminalOutput(raw: String): String =
    raw.replace(ansiEscape, "").filter { it == '\t' || it >= ' ' }.trim()

/** Quotes a value for the device's POSIX shell. */
fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
