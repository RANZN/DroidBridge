package com.ranjan.droidbridge.domain.usecase

import com.ranjan.droidbridge.domain.model.Transfer
import com.ranjan.droidbridge.domain.repository.TransferRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveTransfersUseCase(private val repository: TransferRepository) {
    operator fun invoke(): StateFlow<List<Transfer>> = repository.transfers
}
