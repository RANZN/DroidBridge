package com.ranjan.adbsend.data.repository

import com.ranjan.adbsend.data.adb.AdbClient
import com.ranjan.adbsend.domain.model.Transfer
import com.ranjan.adbsend.domain.model.TransferDirection
import com.ranjan.adbsend.domain.model.TransferStatus
import com.ranjan.adbsend.domain.repository.TransferRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Runs adb push/pull. Transfers to the same device run one at a time (adb is
 * much slower when several copies compete on one USB link); different devices run in parallel.
 */
class AdbTransferRepository(
    private val adb: AdbClient,
    private val scope: CoroutineScope,
    private val stagingRoot: File = File(System.getProperty("java.io.tmpdir"), "ADBSend/drag"),
) : TransferRepository {

    private val nextId = AtomicLong(1)
    private val deviceLocks = ConcurrentHashMap<String, Mutex>()
    private val _transfers = MutableStateFlow<List<Transfer>>(emptyList())
    override val transfers: StateFlow<List<Transfer>> = _transfers.asStateFlow()

    override fun push(serial: String, localPaths: List<String>, remoteDirectory: String) {
        val target = if (remoteDirectory.endsWith("/")) remoteDirectory else "$remoteDirectory/"
        localPaths.forEach { local ->
            enqueue(serial, TransferDirection.Push, local, target)
        }
    }

    override fun pull(serial: String, remotePaths: List<String>, localDirectory: String) {
        File(localDirectory).mkdirs()
        remotePaths.forEach { remote ->
            enqueue(serial, TransferDirection.Pull, remote, localDirectory)
        }
    }

    override suspend fun stageForDrag(serial: String, remotePath: String): Result<String> {
        val dir = File(stagingRoot, UUID.randomUUID().toString()).apply { mkdirs() }
        val transfer = newTransfer(serial, TransferDirection.Pull, remotePath, "Finder (drag)")
        execute(transfer, listOf("-s", serial, "pull", remotePath, dir.absolutePath))
        val status = _transfers.value.first { it.id == transfer.id }.status
        return if (status is TransferStatus.Failed) {
            Result.failure(IllegalStateException(status.reason))
        } else {
            Result.success(File(dir, remotePath.trimEnd('/').substringAfterLast('/')).absolutePath)
        }
    }

    override fun clearFinished() {
        _transfers.update { list -> list.filterNot { it.isFinished } }
    }

    fun cleanStaging() {
        stagingRoot.deleteRecursively()
    }

    private fun enqueue(serial: String, direction: TransferDirection, source: String, destination: String) {
        val transfer = newTransfer(serial, direction, source, destination)
        val command = when (direction) {
            TransferDirection.Push -> listOf("-s", serial, "push", source, destination)
            TransferDirection.Pull -> listOf("-s", serial, "pull", source, destination)
        }
        scope.launch { execute(transfer, command) }
    }

    private fun newTransfer(
        serial: String,
        direction: TransferDirection,
        source: String,
        destination: String,
    ): Transfer {
        val transfer = Transfer(
            id = nextId.getAndIncrement(),
            deviceSerial = serial,
            direction = direction,
            source = source,
            destination = destination,
            status = TransferStatus.Queued,
        )
        _transfers.update { it + transfer }
        return transfer
    }

    private suspend fun execute(transfer: Transfer, command: List<String>) {
        deviceLocks.getOrPut(transfer.deviceSerial) { Mutex() }.withLock {
            setStatus(transfer.id, TransferStatus.Running)
            val status = runCatching { adb.run(command) }.fold(
                onSuccess = { result ->
                    if (result.isSuccess) {
                        TransferStatus.Completed(
                            result.stdout.lines().lastOrNull { it.isNotBlank() }?.trim() ?: "Done"
                        )
                    } else {
                        TransferStatus.Failed(result.errorMessage)
                    }
                },
                onFailure = { TransferStatus.Failed(it.message ?: it.toString()) },
            )
            setStatus(transfer.id, status)
        }
    }

    private fun setStatus(id: Long, status: TransferStatus) {
        _transfers.update { list -> list.map { if (it.id == id) it.copy(status = status) else it } }
    }
}
