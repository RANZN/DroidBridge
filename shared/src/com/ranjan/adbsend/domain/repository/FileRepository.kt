package com.ranjan.adbsend.domain.repository

import com.ranjan.adbsend.domain.model.RemoteFile

interface FileRepository {
    suspend fun list(serial: String, directory: String): Result<List<RemoteFile>>
    suspend fun createDirectory(serial: String, path: String): Result<Unit>
    suspend fun delete(serial: String, path: String): Result<Unit>
}
