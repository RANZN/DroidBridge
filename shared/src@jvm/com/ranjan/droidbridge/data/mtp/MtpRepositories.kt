package com.ranjan.droidbridge.data.mtp

import com.ranjan.droidbridge.data.transfer.TransferEngine
import com.ranjan.droidbridge.data.transfer.TransferProgress
import com.ranjan.droidbridge.domain.model.Device
import com.ranjan.droidbridge.domain.model.MtpStatus
import com.ranjan.droidbridge.domain.model.RemoteFile
import com.ranjan.droidbridge.domain.repository.FileRepository
import com.ranjan.droidbridge.domain.repository.MtpDeviceRepository
import com.ranjan.droidbridge.domain.util.runSuspendCatching
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class LibMtpDeviceRepository(
    private val session: MtpSession,
    private val library: MtpLibrary,
    private val pollInterval: Duration = 3.seconds,
) : MtpDeviceRepository {

    override fun status(): MtpStatus =
        if (session.isAvailable) MtpStatus.Available else MtpStatus.NotInstalled(library.installCommand)

    override fun observeDevices(): Flow<List<Device>> = flow {
        if (!session.isAvailable) {
            emit(emptyList())
            return@flow
        }
        while (true) {
            emit(runSuspendCatching { session.detect() }.getOrDefault(emptyList()))
            delay(pollInterval)
        }
    }.distinctUntilChanged()
}

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

class MtpTransferEngine(private val session: MtpSession) : TransferEngine {
    override fun handles(serial: String): Boolean = MtpDeviceIds.isMtp(serial)

    override suspend fun push(serial: String, localPath: String, remoteDirectory: String, onProgress: (TransferProgress) -> Unit): String =
        session.upload(serial, localPath, remoteDirectory, onProgress)

    override suspend fun pull(serial: String, remotePath: String, localDirectory: String, onProgress: (TransferProgress) -> Unit): String =
        session.download(serial, remotePath, localDirectory, onProgress)
}
