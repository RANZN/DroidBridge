package com.ranjan.droidbridge.data.repository

import com.ranjan.droidbridge.data.mtp.MtpDeviceIds
import com.ranjan.droidbridge.data.transfer.TransferEngine
import com.ranjan.droidbridge.data.transfer.TransferFailedException
import com.ranjan.droidbridge.data.transfer.TransferProgress
import com.ranjan.droidbridge.domain.model.TransferStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class QueuedTransferRepositoryTest {

    private class FakeEngine(private val mtp: Boolean, private val fail: Boolean = false) : TransferEngine {
        val calls = mutableListOf<String>()
        override fun handles(serial: String) = MtpDeviceIds.isMtp(serial) == mtp
        override suspend fun push(serial: String, localPath: String, remoteDirectory: String, onProgress: (TransferProgress) -> Unit): String {
            calls += "push $localPath"
            onProgress(TransferProgress(50, "a.txt"))
            if (fail) throw TransferFailedException("disk full")
            return "pushed"
        }
        override suspend fun pull(serial: String, remotePath: String, localDirectory: String, onProgress: (TransferProgress) -> Unit): String {
            calls += "pull $remotePath"
            return "pulled"
        }
    }

    @Test
    fun `routes each device to its engine and records results`() = runTest {
        val adb = FakeEngine(mtp = false)
        val mtp = FakeEngine(mtp = true, fail = true)
        val repo = QueuedTransferRepository(listOf(adb, mtp), this)
        repo.push("emulator-5554", listOf("/tmp/a.txt"), "/sdcard")
        repo.push(MtpDeviceIds.of(1, 2), listOf("/tmp/b.txt"), "/Internal shared storage")
        val done = repo.transfers.first { list -> list.size == 2 && list.all { it.isFinished } }

        assertEquals(listOf("push /tmp/a.txt"), adb.calls)
        assertEquals(listOf("push /tmp/b.txt"), mtp.calls)
        assertEquals(TransferStatus.Completed("pushed"), done[0].status)
        assertEquals(100, done[0].progressPercent)
        assertEquals(TransferStatus.Failed("disk full"), done[1].status)
        assertEquals(50, done[1].progressPercent)
    }
}
