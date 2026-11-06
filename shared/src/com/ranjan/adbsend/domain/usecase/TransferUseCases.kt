package com.ranjan.adbsend.domain.usecase

import com.ranjan.adbsend.domain.model.Transfer
import com.ranjan.adbsend.domain.repository.TransferRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveTransfersUseCase(private val repository: TransferRepository) {
    operator fun invoke(): StateFlow<List<Transfer>> = repository.transfers
}

class PushFilesUseCase(private val repository: TransferRepository) {
    operator fun invoke(serial: String, localPaths: List<String>, remoteDirectory: String) {
        if (localPaths.isNotEmpty()) repository.push(serial, localPaths, remoteDirectory)
    }
}

class PullFilesUseCase(private val repository: TransferRepository) {
    operator fun invoke(serial: String, remotePaths: List<String>, localDirectory: String) {
        if (remotePaths.isNotEmpty()) repository.pull(serial, remotePaths, localDirectory)
    }
}

class StageFileForDragUseCase(private val repository: TransferRepository) {
    suspend operator fun invoke(serial: String, remotePath: String): Result<String> =
        repository.stageForDrag(serial, remotePath)
}

class ClearFinishedTransfersUseCase(private val repository: TransferRepository) {
    operator fun invoke() = repository.clearFinished()
}
