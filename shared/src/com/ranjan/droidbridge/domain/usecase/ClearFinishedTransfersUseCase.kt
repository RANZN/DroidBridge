package com.ranjan.droidbridge.domain.usecase

import com.ranjan.droidbridge.domain.repository.TransferRepository

class ClearFinishedTransfersUseCase(private val repository: TransferRepository) {
    operator fun invoke() = repository.clearFinished()
}
