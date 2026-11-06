package com.ranjan.droidbridge.data.update

import kotlin.test.Test

/** Opt-in network check: GITHUB_UPDATE_REPO=owner/repo ./kotlin test */
class GitHubUpdateRepositoryLiveTest {
    @Test
    fun `fetches the latest release`() {
        val repo = System.getenv("GITHUB_UPDATE_REPO") ?: return
        val release = kotlinx.coroutines.runBlocking { GitHubUpdateRepository(java.net.http.HttpClient.newHttpClient()).latestRelease(repo) }.getOrThrow()
        println("Latest $repo release: ${release.version} -> ${release.url}")
        kotlin.test.assertTrue(release.url.startsWith("https://github.com/"))
    }
}
