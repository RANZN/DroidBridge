package com.ranjan.droidbridge.domain.usecase

import com.ranjan.droidbridge.domain.model.StagedFile
import com.ranjan.droidbridge.domain.repository.TransferRepository

class DiscardStagedFilesUseCase(private val repository: TransferRepository) {
    operator fun invoke(files: List<StagedFile>) {
        if (files.isNotEmpty()) repository.discardStaged(files)
    }
}
