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
