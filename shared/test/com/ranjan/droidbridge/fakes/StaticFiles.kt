package com.ranjan.droidbridge.fakes

import com.ranjan.droidbridge.domain.model.RemoteFile
import com.ranjan.droidbridge.domain.repository.FileRepository

class StaticFiles(private val files: List<RemoteFile> = emptyList()) : FileRepository {
    override suspend fun list(serial: String, directory: String) = Result.success(files)
    override suspend fun createDirectory(serial: String, path: String) = Result.success(Unit)
    override suspend fun delete(serial: String, path: String) = Result.success(Unit)
    override suspend fun rename(serial: String, from: String, to: String) = Result.success(Unit)
}
