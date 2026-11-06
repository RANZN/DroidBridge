package com.ranjan.droidbridge.data.update

import kotlin.test.Test
import kotlin.test.assertEquals

class GitHubReleaseParserTest {
    @Test
    fun `reads tag, release url and notes`() {
        val json = """
            {"url":"https://api.github.com/x","html_url":"https://github.com/me/DroidBridge/releases/tag/v1.3.0",
             "id":1,"author":{"login":"me","html_url":"https://github.com/me"},
             "tag_name":"v1.3.0","body":"- MTP support\n- \"Faster\" transfers"}
        """.trimIndent()
        val release = GitHubReleaseParser.parse(json, fallbackUrl = "fallback")
        assertEquals("v1.3.0", release.version)
        assertEquals("https://github.com/me/DroidBridge/releases/tag/v1.3.0", release.url)
        assertEquals("- MTP support\n- \"Faster\" transfers", release.notes)
    }
}

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
