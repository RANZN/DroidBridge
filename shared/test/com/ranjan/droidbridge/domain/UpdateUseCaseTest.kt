package com.ranjan.droidbridge.domain

import com.ranjan.droidbridge.domain.model.AppInfo
import com.ranjan.droidbridge.domain.model.ReleaseInfo
import com.ranjan.droidbridge.domain.model.UpdateCheck
import com.ranjan.droidbridge.domain.repository.UpdateRepository
import com.ranjan.droidbridge.domain.usecase.CheckForUpdateUseCase
import com.ranjan.droidbridge.domain.usecase.Versions
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UpdateUseCaseTest {
    private fun repo(version: String) = object : UpdateRepository {
        override suspend fun latestRelease(repository: String) = Result.success(ReleaseInfo(version, "https://x", null))
    }
    private val info = AppInfo("DroidBridge", "1.2.0", "Dev", updateRepository = "me/DroidBridge")

    @Test
    fun `version comparison is numeric`() {
        assertTrue(Versions.isNewer("v1.10.0", "1.9.3"))
        assertTrue(Versions.isNewer("1.2.1", "1.2"))
        assertFalse(Versions.isNewer("1.2.0", "v1.2"))
        assertFalse(Versions.isNewer("1.2.0-beta", "1.2.0"))
    }

    @Test
    fun `reports available and up to date`() = runTest {
        assertIs<UpdateCheck.Available>(CheckForUpdateUseCase(repo("v1.3.0"), info)().getOrThrow())
        assertEquals(UpdateCheck.UpToDate("1.2.0"), CheckForUpdateUseCase(repo("1.2.0"), info)().getOrThrow())
    }

    @Test
    fun `fails clearly when no repository is configured`() = runTest {
        val result = CheckForUpdateUseCase(repo("9.0"), info.copy(updateRepository = null))()
        assertTrue(result.isFailure)
    }
}
