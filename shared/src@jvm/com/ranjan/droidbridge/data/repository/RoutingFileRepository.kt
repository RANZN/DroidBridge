package com.ranjan.droidbridge.data.repository

import com.ranjan.droidbridge.data.mtp.MtpDeviceIds
import com.ranjan.droidbridge.domain.model.RemoteFile
import com.ranjan.droidbridge.domain.repository.FileRepository

/** Sends each file operation to the ADB or MTP implementation, based on the device id. */
class RoutingFileRepository(
    private val adb: FileRepository,
    private val mtp: FileRepository,
) : FileRepository {
    private fun forDevice(serial: String) = if (MtpDeviceIds.isMtp(serial)) mtp else adb

    override suspend fun list(serial: String, directory: String): Result<List<RemoteFile>> = forDevice(serial).list(serial, directory)
    override suspend fun createDirectory(serial: String, path: String) = forDevice(serial).createDirectory(serial, path)
    override suspend fun delete(serial: String, path: String) = forDevice(serial).delete(serial, path)
    override suspend fun rename(serial: String, from: String, to: String) = forDevice(serial).rename(serial, from, to)
}
