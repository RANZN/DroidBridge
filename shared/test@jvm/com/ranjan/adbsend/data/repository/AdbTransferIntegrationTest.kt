package com.ranjan.adbsend.data.repository

import com.ranjan.adbsend.data.adb.AdbClient
import com.ranjan.adbsend.data.adb.AdbLocator
import com.ranjan.adbsend.domain.model.TransferStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.RandomAccessFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pushes real files to a connected device. Opt-in: ADB_INTEGRATION=1 and ADB_SERIAL=<serial>.
 */
class AdbTransferIntegrationTest {

    @Test
    fun `push several files reports progress and completes`() {
        if (System.getenv("ADB_INTEGRATION") != "1") return
        val serial = System.getenv("ADB_SERIAL") ?: return
        val adb = AdbClient(AdbLocator())
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repo = AdbTransferRepository(adb, scope)
        val dir = File(System.getProperty("java.io.tmpdir"), "adbsend-it").apply { mkdirs() }
        val files = (1..3).map { i ->
            File(dir, "it-$i.bin").apply { RandomAccessFile(this, "rw").use { it.setLength(400L * 1024 * 1024) } }
        }
        val seenPercents = mutableSetOf<Int>()
        runBlocking {
            val watcher = launch {
                repo.transfers.collect { list -> list.mapNotNullTo(seenPercents) { it.progressPercent } }
            }
            try {
                repo.push(serial, files.map { it.absolutePath }, "/sdcard/Download/adbsend-it")
                val done = withTimeout(120_000) {
                    repo.transfers.first { list -> list.size == 3 && list.all { it.isFinished } }
                }
                done.forEach { assertTrue(it.status is TransferStatus.Completed, "${it.source}: ${it.status}") }
                println("Progress values seen: ${seenPercents.sorted()}")
                assertTrue(seenPercents.any { it in 1..99 }, "expected intermediate progress, saw $seenPercents")
                assertEquals(setOf(100), done.map { it.progressPercent }.toSet())
            } finally {
                watcher.cancel()
                adb.shell(serial, "rm -rf /sdcard/Download/adbsend-it")
                dir.deleteRecursively()
                scope.cancel()
            }
        }
    }
}
