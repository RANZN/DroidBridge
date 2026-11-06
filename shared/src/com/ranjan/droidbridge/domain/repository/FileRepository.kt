package com.ranjan.droidbridge.domain.repository

import com.ranjan.droidbridge.domain.model.RemoteFile

interface FileRepository {
    suspend fun list(serial: String, directory: String): Result<List<RemoteFile>>
    suspend fun createDirectory(serial: String, path: String): Result<Unit>
    suspend fun delete(serial: String, path: String): Result<Unit>

    /** Moves [from] to [to]; fails if [to] already exists. */
    suspend fun rename(serial: String, from: String, to: String): Result<Unit>
}
