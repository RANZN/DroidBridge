package com.ranjan.droidbridge.domain.usecase

import com.ranjan.droidbridge.domain.model.RemoteFile
import com.ranjan.droidbridge.domain.repository.FileRepository

class ListRemoteFilesUseCase(private val repository: FileRepository) {
    /** Directories first, then files, both alphabetical (case-insensitive). */
    suspend operator fun invoke(serial: String, directory: String): Result<List<RemoteFile>> =
        repository.list(serial, directory).map { files ->
            files.sortedWith(compareBy<RemoteFile> { !it.isDirectory }.thenBy { it.name.lowercase() })
        }
}
