package com.ranjan.droidbridge.data.mtp

/** Pure helpers for DroidBridge's MTP paths: "/<storage name>/<folder>/<file>". */
object MtpPaths {
    fun segments(path: String): List<String> = path.split('/').filter { it.isNotEmpty() }

    fun join(segments: List<String>): String = "/" + segments.joinToString("/")

    /** Gives duplicate storage names a suffix so every path stays unique. */
    fun uniqueStorageNames(names: List<String>): List<String> {
        val seen = mutableMapOf<String, Int>()
        return names.map { raw ->
            val base = raw.ifBlank { "Storage" }.replace('/', '-')
            val count = (seen[base] ?: 0) + 1
            seen[base] = count
            if (count == 1) base else "$base ($count)"
        }
    }
}
