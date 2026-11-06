package com.ranjan.droidbridge.data.repository

import com.ranjan.droidbridge.data.transfer.TransferEngine
import com.ranjan.droidbridge.data.transfer.TransferProgress
import com.ranjan.droidbridge.domain.model.StagedFile
import com.ranjan.droidbridge.domain.model.Transfer
import com.ranjan.droidbridge.domain.model.TransferDirection
import com.ranjan.droidbridge.domain.model.TransferStatus
import com.ranjan.droidbridge.domain.repository.TransferRepository
import kotlinx.coroutines.CancellationException
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
 * Transfer queue shared by all connection types. Transfers to the same device run one at a
 * time (copies competing on one USB link are slower); different devices run in parallel.
 * The actual copying is delegated to the [TransferEngine] that handles the device.
 */
class QueuedTransferRepository(
    private val engines: List<TransferEngine>,
    private val scope: CoroutineScope,
    private val stagingRoot: File = File(System.getProperty("java.io.tmpdir"), "DroidBridge/drag"),
) : TransferRepository {

    private val nextId = AtomicLong(1)
    private val deviceLocks = ConcurrentHashMap<String, Mutex>()
    private val _transfers = MutableStateFlow<List<Transfer>>(emptyList())
    override val transfers: StateFlow<List<Transfer>> = _transfers.asStateFlow()

    override fun push(serial: String, localPaths: List<String>, remoteDirectory: String) {
        localPaths.forEach { local ->
            val transfer = newTransfer(serial, TransferDirection.Push, local, remoteDirectory)
            scope.launch { execute(transfer) { engine, progress -> engine.push(serial, local, remoteDirectory, progress) } }
        }
    }

    override fun pull(serial: String, remotePaths: List<String>, localDirectory: String) {
        File(localDirectory).mkdirs()
        remotePaths.forEach { remote ->
            val transfer = newTransfer(serial, TransferDirection.Pull, remote, localDirectory)
            scope.launch { execute(transfer) { engine, progress -> engine.pull(serial, remote, localDirectory, progress) } }
        }
    }

    override suspend fun stageForDrag(serial: String, remotePath: String): Result<StagedFile> {
        val dir = File(stagingRoot, UUID.randomUUID().toString()).apply { mkdirs() }
        val transfer = newTransfer(serial, TransferDirection.Pull, remotePath, "Finder (drag)")
        val staged = StagedFile(transfer.id, File(dir, remotePath.trimEnd('/').substringAfterLast('/')).absolutePath)
        try {
            execute(transfer) { engine, progress -> engine.pull(serial, remotePath, dir.absolutePath, progress) }
        } catch (e: CancellationException) {
            // Drag was cancelled or dropped back on the app: leave no trace.
            discardStaged(listOf(staged))
            throw e
        }
        val status = _transfers.value.firstOrNull { it.id == transfer.id }?.status
        return if (status is TransferStatus.Failed) {
            Result.failure(IllegalStateException(status.reason))
        } else {
            Result.success(staged)
        }
    }

    override fun discardStaged(files: List<StagedFile>) {
        val ids = files.map { it.transferId }.toSet()
        _transfers.update { list -> list.filterNot { it.id in ids } }
        val root = stagingRoot.canonicalFile
        files.forEach { file ->
            // Each staged file lives in its own random folder directly under the staging root.
            val folder = File(file.localPath).canonicalFile.parentFile ?: return@forEach
            if (folder.parentFile == root) folder.deleteRecursively()
        }
    }

    override fun clearFinished() {
        _transfers.update { list -> list.filterNot { it.isFinished } }
    }

    fun cleanStaging() {
        stagingRoot.deleteRecursively()
    }

    private fun newTransfer(serial: String, direction: TransferDirection, source: String, destination: String): Transfer {
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

    private suspend fun execute(
        transfer: Transfer,
        copy: suspend (TransferEngine, (TransferProgress) -> Unit) -> String,
    ) {
        deviceLocks.getOrPut(transfer.deviceSerial) { Mutex() }.withLock {
            setStatus(transfer.id, TransferStatus.Running)
            var lastPercent = -1
            val onProgress: (TransferProgress) -> Unit = { progress ->
                if (progress.percent != lastPercent) { // avoid flooding the UI
                    lastPercent = progress.percent
                    updateTransfer(transfer.id) { it.copy(progressPercent = progress.percent, currentItem = progress.item) }
                }
            }
            val status = try {
                val engine = engines.firstOrNull { it.handles(transfer.deviceSerial) }
                    ?: error("No transfer engine for ${transfer.deviceSerial}")
                TransferStatus.Completed(copy(engine, onProgress))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                TransferStatus.Failed(e.message ?: e.toString())
            }
            setStatus(transfer.id, status)
        }
    }

    private fun setStatus(id: Long, status: TransferStatus) = updateTransfer(id) {
        it.copy(status = status, progressPercent = if (status is TransferStatus.Completed) 100 else it.progressPercent)
    }

    private fun updateTransfer(id: Long, change: (Transfer) -> Transfer) {
        _transfers.update { list -> list.map { if (it.id == id) change(it) else it } }
    }
}
