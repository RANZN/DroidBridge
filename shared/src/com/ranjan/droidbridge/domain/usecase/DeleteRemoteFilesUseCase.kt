package com.ranjan.droidbridge.domain.usecase

import com.ranjan.droidbridge.domain.util.runSuspendCatching
import com.ranjan.droidbridge.domain.repository.FileRepository

class DeleteRemoteFilesUseCase(private val repository: FileRepository) {
    suspend operator fun invoke(serial: String, paths: List<String>): Result<Unit> = runSuspendCatching {
        paths.forEach { path ->
            require(path !in RemotePaths.protectedPaths) { "Refusing to delete $path" }
            repository.delete(serial, path).getOrThrow()
        }
    }
}
