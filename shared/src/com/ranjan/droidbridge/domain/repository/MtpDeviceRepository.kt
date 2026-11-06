package com.ranjan.droidbridge.domain.repository

import com.ranjan.droidbridge.domain.model.Device
import com.ranjan.droidbridge.domain.model.MtpStatus
import kotlinx.coroutines.flow.Flow

interface MtpDeviceRepository {
    fun status(): MtpStatus

    /** Emits MTP devices (phones in "File transfer" USB mode) whenever the list changes. */
    fun observeDevices(): Flow<List<Device>>
}
