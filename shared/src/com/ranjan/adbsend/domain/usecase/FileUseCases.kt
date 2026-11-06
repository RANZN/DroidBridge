package com.ranjan.adbsend.domain.usecase

import com.ranjan.adbsend.domain.model.RemoteFile
import com.ranjan.adbsend.domain.repository.FileRepository

class ListRemoteFilesUseCase(private val repository: FileRepository) {
    /** Directories first, then files, both alphabetical (case-insensitive). */
    suspend operator fun invoke(serial: String, directory: String): Result<List<RemoteFile>> =
        repository.list(serial, directory).map { files ->
            files.sortedWith(compareBy<RemoteFile> { !it.isDirectory }.thenBy { it.name.lowercase() })
        }
}

class CreateRemoteFolderUseCase(private val repository: FileRepository) {
    suspend operator fun invoke(serial: String, parent: String, name: String): Result<Unit> {
        val clean = name.trim()
        if (clean.isEmpty() || clean.contains('/')) {
            return Result.failure(IllegalArgumentException("Folder name must not be empty or contain '/'"))
        }
        return repository.createDirectory(serial, RemotePaths.child(parent, clean))
    }
}

class DeleteRemoteFilesUseCase(private val repository: FileRepository) {
    suspend operator fun invoke(serial: String, paths: List<String>): Result<Unit> = runCatching {
        paths.forEach { path ->
            require(path !in RemotePaths.protectedPaths) { "Refusing to delete $path" }
            repository.delete(serial, path).getOrThrow()
        }
    }
}
