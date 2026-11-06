package com.ranjan.adbsend.domain.repository

import com.ranjan.adbsend.domain.model.AdbStatus
import com.ranjan.adbsend.domain.model.Device
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun adbStatus(): AdbStatus

    /** Emits the current device list whenever it changes. */
    fun observeDevices(): Flow<List<Device>>
}
