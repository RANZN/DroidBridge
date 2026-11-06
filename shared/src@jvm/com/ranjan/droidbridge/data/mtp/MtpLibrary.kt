package com.ranjan.droidbridge.data.mtp

import com.sun.jna.Native
import java.io.File

/** Finds and loads libmtp (installed with `brew install libmtp`). */
class MtpLibrary(
    private val env: Map<String, String> = System.getenv(),
) {
    val installCommand = "brew install libmtp"

    private val loaded: LibMtpNative? by lazy {
        val path = candidates().firstOrNull { File(it).exists() } ?: return@lazy null
        runCatching { Native.load(path, LibMtpNative::class.java).also { it.LIBMTP_Init() } }.getOrNull()
    }

    val native: LibMtpNative? get() = loaded

    internal fun candidates(): List<String> = buildList {
        env["LIBMTP_PATH"]?.let(::add)
        add("/opt/homebrew/lib/libmtp.dylib") // Apple Silicon Homebrew
        add("/usr/local/lib/libmtp.dylib") // Intel Homebrew
        add("/opt/local/lib/libmtp.dylib") // MacPorts
    }
}
