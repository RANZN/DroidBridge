package com.ranjan.droidbridge.data.adb

import com.ranjan.droidbridge.domain.model.Device
import com.ranjan.droidbridge.domain.model.DeviceState
import com.ranjan.droidbridge.domain.model.RemoteFile
import com.ranjan.droidbridge.domain.model.Transport

object AdbParsers {

    // "[ 45%] /sdcard/file.bin" or, for folders, "[ 35%] /sdcard/dir/b.bin: 71%"
    private val progressLine = Regex("""^\[\s*(\d{1,3})%\]\s+(.+?)(?::\s*\d{1,3}%)?$""")

    fun parseProgress(line: String): AdbProgress? {
        val match = progressLine.matchEntire(line.trim()) ?: return null
        val percent = match.groupValues[1].toInt().coerceIn(0, 100)
        return AdbProgress(percent, match.groupValues[2])
    }

    /** Parses the output of `adb devices -l`. */
    fun parseDevices(output: String): List<Device> = output.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("List of devices") && !it.startsWith("*") }
        .mapNotNull { line ->
            val tokens = line.split(Regex("\\s+"))
            if (tokens.size < 2) return@mapNotNull null
            val props = tokens.drop(2).mapNotNull { token ->
                token.split(':', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] }
            }.toMap()
            Device(
                serial = tokens[0],
                state = when (tokens[1]) {
                    "device" -> DeviceState.Online
                    "unauthorized" -> DeviceState.Unauthorized
                    "offline" -> DeviceState.Offline
                    else -> DeviceState.Unknown
                },
                model = props["model"],
                product = props["product"],
                transport = Transport.Adb,
            )
        }
        .toList()

    /** Separator used in [LIST_FORMAT]; the name comes last so it may contain anything but newlines. */
    const val LIST_FORMAT = "%F|%s|%Y|%n"

    /** Parses lines produced by `stat -L -c '%F|%s|%Y|%n'`. */
    fun parseListing(output: String): List<RemoteFile> = output.lineSequence()
        .mapNotNull { line ->
            val parts = line.split('|', limit = 4)
            if (parts.size != 4) return@mapNotNull null
            val path = parts[3].trimEnd('/')
            val name = path.substringAfterLast('/')
            if (name.isEmpty() || name == "." || name == "..") return@mapNotNull null
            RemoteFile(
                path = path,
                name = name,
                isDirectory = parts[0] == "directory",
                sizeBytes = parts[1].toLongOrNull() ?: 0L,
                modifiedEpochSeconds = parts[2].toLongOrNull() ?: 0L,
            )
        }
        .toList()
}
