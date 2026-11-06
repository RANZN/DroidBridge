package com.ranjan.droidbridge.fakes

import com.ranjan.droidbridge.domain.model.StagedFile
import com.ranjan.droidbridge.domain.model.Transfer
import com.ranjan.droidbridge.domain.repository.TransferRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

open class NoopTransfers : TransferRepository {
    override val transfers: StateFlow<List<Transfer>> = MutableStateFlow(emptyList())
    override fun push(serial: String, localPaths: List<String>, remoteDirectory: String) = Unit
    override fun pull(serial: String, remotePaths: List<String>, localDirectory: String) = Unit
    override suspend fun stageForDrag(serial: String, remotePath: String): Result<StagedFile> =
        Result.failure(UnsupportedOperationException())
    override fun discardStaged(files: List<StagedFile>) = Unit
    override fun clearFinished() = Unit
}
