package com.ranjan.adbsend.domain.model

enum class TransferDirection { Push, Pull }

sealed interface TransferStatus {
    data object Queued : TransferStatus
    data object Running : TransferStatus
    data class Completed(val summary: String) : TransferStatus
    data class Failed(val reason: String) : TransferStatus
}

/** A single adb push or pull operation. */
data class Transfer(
    val id: Long,
    val deviceSerial: String,
    val direction: TransferDirection,
    val source: String,
    val destination: String,
    val status: TransferStatus,
    /** Overall progress 0-100 while running, when adb reports it. */
    val progressPercent: Int? = null,
    /** File currently being copied (useful for folders). */
    val currentItem: String? = null,
) {
    val isFinished: Boolean
        get() = status is TransferStatus.Completed || status is TransferStatus.Failed
}
