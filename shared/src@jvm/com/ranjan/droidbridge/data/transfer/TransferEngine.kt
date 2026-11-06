package com.ranjan.droidbridge.data.transfer

/** Progress of one transfer: overall [percent] and the file currently being copied. */
data class TransferProgress(val percent: Int, val item: String?)

/**
 * Copies files for one kind of connection (ADB or MTP). The queue in
 * [QueuedTransferRepository] owns ordering, records and staging; engines only copy.
 * Both methods return a short success summary and throw on failure.
 */
interface TransferEngine {
    fun handles(serial: String): Boolean

    suspend fun push(serial: String, localPath: String, remoteDirectory: String, onProgress: (TransferProgress) -> Unit): String

    suspend fun pull(serial: String, remotePath: String, localDirectory: String, onProgress: (TransferProgress) -> Unit): String
}

class TransferFailedException(message: String) : Exception(message)
