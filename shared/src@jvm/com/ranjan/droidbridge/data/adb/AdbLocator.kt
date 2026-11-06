package com.ranjan.droidbridge.data.adb

import java.io.File

/**
 * Finds the adb binary. Apps launched from Finder/Dock do not inherit the shell PATH,
 * so the usual SDK and Homebrew locations are checked explicitly.
 */
class AdbLocator(
    private val env: Map<String, String> = System.getenv(),
    private val home: String = System.getProperty("user.home"),
) {
    fun locate(): String? = candidates().firstOrNull { File(it).canExecute() }

    internal fun candidates(): List<String> = buildList {
        env["ADB_PATH"]?.let(::add)
        listOf("ANDROID_HOME", "ANDROID_SDK_ROOT").forEach { key ->
            env[key]?.let { add("$it/platform-tools/adb") }
        }
        add("$home/Library/Android/sdk/platform-tools/adb")
        add("/opt/homebrew/bin/adb")
        add("/usr/local/bin/adb")
        env["PATH"]?.split(File.pathSeparator)?.forEach { add("$it/adb") }
    }.distinct()
}
