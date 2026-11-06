package com.ranjan.droidbridge.domain.repository

import com.ranjan.droidbridge.domain.model.AdbStatus
import com.ranjan.droidbridge.domain.model.Device
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun adbStatus(): AdbStatus

    /** Emits the current device list whenever it changes. */
    fun observeDevices(): Flow<List<Device>>
}
