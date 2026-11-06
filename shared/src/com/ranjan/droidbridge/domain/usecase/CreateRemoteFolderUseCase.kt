package com.ranjan.droidbridge.domain.usecase

import com.ranjan.droidbridge.domain.repository.FileRepository

class CreateRemoteFolderUseCase(private val repository: FileRepository) {
    suspend operator fun invoke(serial: String, parent: String, name: String): Result<Unit> {
        val clean = name.trim()
        if (clean.isEmpty() || clean.contains('/')) {
            return Result.failure(IllegalArgumentException("Folder name must not be empty or contain '/'"))
        }
        return repository.createDirectory(serial, RemotePaths.child(parent, clean))
    }
}
