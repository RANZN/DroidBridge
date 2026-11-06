package com.ranjan.adbsend.data.repository

import com.ranjan.adbsend.data.adb.AdbClient
import com.ranjan.adbsend.domain.util.runSuspendCatching
import com.ranjan.adbsend.data.adb.AdbParsers
import com.ranjan.adbsend.data.adb.shellQuote
import com.ranjan.adbsend.domain.model.RemoteFile
import com.ranjan.adbsend.domain.repository.FileRepository

class AdbFileRepository(private val adb: AdbClient) : FileRepository {

    override suspend fun list(serial: String, directory: String): Result<List<RemoteFile>> = runSuspendCatching {
        // Trailing slash makes find follow a symlinked directory such as /sdcard.
        val dir = if (directory.endsWith("/")) directory else "$directory/"
        val command = "find ${shellQuote(dir)} -mindepth 1 -maxdepth 1 " +
            "-exec stat -L -c ${shellQuote(AdbParsers.LIST_FORMAT)} {} +"
        val result = adb.shell(serial, command)
        val files = AdbParsers.parseListing(result.stdout)
        // find exits non-zero if any entry is unreadable; keep partial results when we have them.
        if (!result.isSuccess && files.isEmpty() && result.stderr.isNotBlank()) {
            error(result.errorMessage)
        }
        files
    }

    override suspend fun createDirectory(serial: String, path: String): Result<Unit> = runSuspendCatching {
        val result = adb.shell(serial, "mkdir -p ${shellQuote(path)}")
        if (!result.isSuccess || result.stdout.isNotBlank()) error(result.errorMessage)
    }

    override suspend fun delete(serial: String, path: String): Result<Unit> = runSuspendCatching {
        val result = adb.shell(serial, "rm -rf ${shellQuote(path)}")
        if (!result.isSuccess || result.stdout.isNotBlank()) error(result.errorMessage)
    }

    override suspend fun rename(serial: String, from: String, to: String): Result<Unit> = runSuspendCatching {
        val target = shellQuote(to)
        // Never overwrite an existing item silently.
        val command = "if [ -e $target ]; then echo 'An item with that name already exists' >&2; exit 1; fi; " +
            "mv ${shellQuote(from)} $target"
        val result = adb.shell(serial, command)
        if (!result.isSuccess || result.stdout.isNotBlank()) error(result.errorMessage)
    }
}
