package com.ranjan.droidbridge.data.update

import com.ranjan.droidbridge.domain.model.ReleaseInfo

/** Minimal parser for the three fields DroidBridge needs from a GitHub release. */
object GitHubReleaseParser {
    fun parse(json: String, fallbackUrl: String): ReleaseInfo {
        val tag = stringField(json, "tag_name") ?: error("Unexpected response from GitHub")
        // The release's own html_url is the first one in the object (the author's comes later).
        val url = stringField(json, "html_url") ?: fallbackUrl
        val notes = stringField(json, "body")?.takeIf { it.isNotBlank() }
        return ReleaseInfo(tag, url, notes)
    }

    private fun stringField(json: String, name: String): String? {
        val match = Regex("\"" + Regex.escape(name) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").find(json) ?: return null
        return unescape(match.groupValues[1])
    }

    private fun unescape(value: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '\\' && i + 1 < value.length) {
                when (val n = value[i + 1]) {
                    'n' -> out.append('\n')
                    'r' -> out.append('\r')
                    't' -> out.append('\t')
                    'u' -> if (i + 5 < value.length) {
                        out.append(value.substring(i + 2, i + 6).toInt(16).toChar())
                        i += 4
                    }
                    else -> out.append(n)
                }
                i += 2
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }
}
