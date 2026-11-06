package com.ranjan.adbsend.data.repository

import com.ranjan.adbsend.data.adb.AdbClient
import com.ranjan.adbsend.data.adb.AdbParsers
import com.ranjan.adbsend.domain.model.AdbStatus
import com.ranjan.adbsend.domain.model.Device
import com.ranjan.adbsend.domain.repository.DeviceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class AdbDeviceRepository(
    private val adb: AdbClient,
    private val pollInterval: Duration = 1500.milliseconds,
) : DeviceRepository {

    override fun adbStatus(): AdbStatus =
        adb.adbPath?.let { AdbStatus.Available(it) } ?: AdbStatus.NotFound

    override fun observeDevices(): Flow<List<Device>> = flow {
        if (adb.adbPath == null) {
            emit(emptyList())
            return@flow
        }
        adb.run("start-server")
        while (true) {
            val result = runCatching { adb.run("devices", "-l") }.getOrNull()
            emit(if (result?.isSuccess == true) AdbParsers.parseDevices(result.stdout) else emptyList())
            delay(pollInterval)
        }
    }.distinctUntilChanged()
}
