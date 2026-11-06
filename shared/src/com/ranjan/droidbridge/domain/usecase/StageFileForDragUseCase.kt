package com.ranjan.droidbridge.domain.usecase

import com.ranjan.droidbridge.domain.model.StagedFile
import com.ranjan.droidbridge.domain.repository.TransferRepository

class StageFileForDragUseCase(private val repository: TransferRepository) {
    suspend operator fun invoke(serial: String, remotePath: String): Result<StagedFile> =
        repository.stageForDrag(serial, remotePath)
}
