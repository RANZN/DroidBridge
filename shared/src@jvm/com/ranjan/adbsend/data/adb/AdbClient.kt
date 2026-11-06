package com.ranjan.adbsend.data.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class AdbResult(val exitCode: Int, val stdout: String, val stderr: String) {
    val isSuccess: Boolean get() = exitCode == 0

    /** Best human-readable error message adb produced. */
    val errorMessage: String
        get() = (stderr.ifBlank { stdout }).lines().lastOrNull { it.isNotBlank() }?.trim()
            ?: "adb exited with code $exitCode"
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

    /** Runs a command inside `adb shell` on [serial]. */
    suspend fun shell(serial: String, command: String): AdbResult =
        run("-s", serial, "shell", command)
}

/** Quotes a value for the device's POSIX shell. */
fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
