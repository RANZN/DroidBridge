package com.ranjan.droidbridge.domain.usecase

import com.ranjan.droidbridge.domain.repository.FileRepository

class RenameRemoteFileUseCase(private val repository: FileRepository) {
    suspend operator fun invoke(serial: String, path: String, newName: String): Result<String> {
        val clean = newName.trim()
        if (clean.isEmpty() || clean.contains('/') || clean == "." || clean == "..") {
            return Result.failure(IllegalArgumentException("Name must not be empty or contain '/'"))
        }
        if (clean == RemotePaths.name(path)) return Result.success(path)
        if (path in RemotePaths.protectedPaths) {
            return Result.failure(IllegalArgumentException("Refusing to rename $path"))
        }
        val parent = RemotePaths.parent(path) ?: return Result.failure(IllegalArgumentException("Cannot rename /"))
        val target = RemotePaths.child(parent, clean)
        return repository.rename(serial, path, target).map { target }
    }
}
