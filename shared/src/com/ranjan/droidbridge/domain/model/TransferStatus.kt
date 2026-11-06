package com.ranjan.droidbridge.domain.model

sealed interface TransferStatus {
    data object Queued : TransferStatus
    data object Running : TransferStatus
    data class Completed(val summary: String) : TransferStatus
    data class Failed(val reason: String) : TransferStatus
}
