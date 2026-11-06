package com.ranjan.droidbridge.data.mtp

import com.ranjan.droidbridge.data.transfer.TransferEngine
import com.ranjan.droidbridge.data.transfer.TransferProgress

class MtpTransferEngine(private val session: MtpSession) : TransferEngine {
    override fun handles(serial: String): Boolean = MtpDeviceIds.isMtp(serial)

    override suspend fun push(serial: String, localPath: String, remoteDirectory: String, onProgress: (TransferProgress) -> Unit): String =
        session.upload(serial, localPath, remoteDirectory, onProgress)

    override suspend fun pull(serial: String, remotePath: String, localDirectory: String, onProgress: (TransferProgress) -> Unit): String =
        session.download(serial, remotePath, localDirectory, onProgress)
}
