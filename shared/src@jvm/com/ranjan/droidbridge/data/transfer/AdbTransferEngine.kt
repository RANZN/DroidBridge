package com.ranjan.droidbridge.data.transfer

import com.ranjan.droidbridge.data.adb.AdbClient
import com.ranjan.droidbridge.data.adb.AdbParsers
import com.ranjan.droidbridge.data.adb.shellQuote
import com.ranjan.droidbridge.data.mtp.MtpDeviceIds
import com.ranjan.droidbridge.domain.util.runSuspendCatching

/** adb push / adb pull, with progress parsed from adb's terminal output. */
class AdbTransferEngine(private val adb: AdbClient) : TransferEngine {

    override fun handles(serial: String): Boolean = !MtpDeviceIds.isMtp(serial)

    override suspend fun push(
        serial: String,
        localPath: String,
        remoteDirectory: String,
        onProgress: (TransferProgress) -> Unit,
    ): String {
        val target = if (remoteDirectory.endsWith("/")) remoteDirectory else "$remoteDirectory/"
        // adb push into a missing folder fails, e.g. if it was deleted on the device meanwhile.
        runSuspendCatching { adb.shell(serial, "mkdir -p ${shellQuote(target)}") }
        return run(listOf("-s", serial, "push", localPath, target), onProgress)
    }

    override suspend fun pull(
        serial: String,
        remotePath: String,
        localDirectory: String,
        onProgress: (TransferProgress) -> Unit,
    ): String = run(listOf("-s", serial, "pull", remotePath, localDirectory), onProgress)

    private suspend fun run(command: List<String>, onProgress: (TransferProgress) -> Unit): String {
        val result = adb.runStreaming(command) { line ->
            AdbParsers.parseProgress(line)?.let { onProgress(TransferProgress(it.percent, it.item.substringAfterLast('/'))) }
        }
        if (!result.isSuccess) throw TransferFailedException(result.errorMessage)
        return result.stdout.lines().lastOrNull { it.isNotBlank() }?.trim() ?: "Done"
    }
}
