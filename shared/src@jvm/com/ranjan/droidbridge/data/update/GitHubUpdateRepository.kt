package com.ranjan.droidbridge.data.update

import com.ranjan.droidbridge.domain.model.ReleaseInfo
import com.ranjan.droidbridge.domain.repository.UpdateRepository
import com.ranjan.droidbridge.domain.util.runSuspendCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Reads the latest release from the GitHub Releases API (no token needed for public repos). */
class GitHubUpdateRepository(
    private val client: HttpClient,
) : UpdateRepository {

    override suspend fun latestRelease(repository: String): Result<ReleaseInfo> = runSuspendCatching {
        withContext(Dispatchers.IO) {
            val request = HttpRequest.newBuilder(URI("https://api.github.com/repos/$repository/releases/latest"))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "DroidBridge")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            when (response.statusCode()) {
                200 -> GitHubReleaseParser.parse(response.body(), fallbackUrl = "https://github.com/$repository/releases")
                404 -> error("No releases have been published yet.")
                else -> error("GitHub returned ${response.statusCode()}. Try again later.")
            }
        }
    }
}

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
