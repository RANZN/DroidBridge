package com.ranjan.droidbridge.data.transfer

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
