package com.ranjan.droidbridge.domain

import com.ranjan.droidbridge.domain.model.RemoteFile
import com.ranjan.droidbridge.domain.repository.FileRepository
import com.ranjan.droidbridge.domain.usecase.RenameRemoteFileUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RenameRemoteFileUseCaseTest {
    private class FakeRepo : FileRepository {
        val renames = mutableListOf<Pair<String, String>>()
        override suspend fun list(serial: String, directory: String) = Result.success(emptyList<RemoteFile>())
        override suspend fun createDirectory(serial: String, path: String) = Result.success(Unit)
        override suspend fun delete(serial: String, path: String) = Result.success(Unit)
        override suspend fun rename(serial: String, from: String, to: String): Result<Unit> {
            renames += from to to
            return Result.success(Unit)
        }
    }

    @Test
    fun `renames within the same folder`() = runTest {
        val repo = FakeRepo()
        val result = RenameRemoteFileUseCase(repo)("s", "/sdcard/DCIM/a.jpg", "  b.jpg ")
        assertEquals("/sdcard/DCIM/b.jpg", result.getOrThrow())
        assertEquals(listOf("/sdcard/DCIM/a.jpg" to "/sdcard/DCIM/b.jpg"), repo.renames)
    }

    @Test
    fun `rejects invalid names and protected paths`() = runTest {
        val repo = FakeRepo()
        val useCase = RenameRemoteFileUseCase(repo)
        assertTrue(useCase("s", "/sdcard/a", "x/y").isFailure)
        assertTrue(useCase("s", "/sdcard/a", " ").isFailure)
        assertTrue(useCase("s", "/sdcard", "other").isFailure)
        assertTrue(repo.renames.isEmpty())
    }

    @Test
    fun `same name is a no-op`() = runTest {
        val repo = FakeRepo()
        assertEquals("/sdcard/a", RenameRemoteFileUseCase(repo)("s", "/sdcard/a", "a").getOrThrow())
        assertTrue(repo.renames.isEmpty())
    }
}
