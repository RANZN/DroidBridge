package com.ranjan.droidbridge.data.adb

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
