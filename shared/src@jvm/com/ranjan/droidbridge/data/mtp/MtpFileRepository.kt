package com.ranjan.droidbridge.data.mtp

import com.ranjan.droidbridge.domain.model.RemoteFile
import com.ranjan.droidbridge.domain.repository.FileRepository
import com.ranjan.droidbridge.domain.util.runSuspendCatching

class MtpFileRepository(private val session: MtpSession) : FileRepository {
    override suspend fun list(serial: String, directory: String): Result<List<RemoteFile>> =
        runSuspendCatching { session.list(serial, directory) }

    override suspend fun createDirectory(serial: String, path: String): Result<Unit> =
        runSuspendCatching { session.createFolder(serial, path) }

    override suspend fun delete(serial: String, path: String): Result<Unit> =
        runSuspendCatching { session.delete(serial, path) }

    override suspend fun rename(serial: String, from: String, to: String): Result<Unit> =
        runSuspendCatching { session.rename(serial, from, to) }
}
