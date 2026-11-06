package com.ranjan.adbsend.domain.repository

import com.ranjan.adbsend.domain.model.StagedFile
import com.ranjan.adbsend.domain.model.Transfer
import kotlinx.coroutines.flow.StateFlow

interface TransferRepository {
    val transfers: StateFlow<List<Transfer>>

    /** Queues `adb push` for each local path into [remoteDirectory]. */
    fun push(serial: String, localPaths: List<String>, remoteDirectory: String)

    /** Queues `adb pull` for each remote path into [localDirectory]. */
    fun pull(serial: String, remotePaths: List<String>, localDirectory: String)

    /**
     * Pulls [remotePath] into a private staging folder and returns the local copy.
     * Used when dragging device files out to Finder, which needs a real local file.
     */
    suspend fun stageForDrag(serial: String, remotePath: String): Result<StagedFile>

    /**
     * Forgets staged files whose drag was never delivered: removes their transfer entries and
     * temporary copies. Cancelling a running [stageForDrag] cleans up the same way.
     */
    fun discardStaged(files: List<StagedFile>)

    fun clearFinished()
}
