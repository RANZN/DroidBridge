package com.ranjan.droidbridge.fakes

import com.ranjan.droidbridge.domain.model.Device
import com.ranjan.droidbridge.domain.model.MtpStatus
import com.ranjan.droidbridge.domain.repository.MtpDeviceRepository
import kotlinx.coroutines.flow.Flow

class FakeMtpDevices(private val devices: Flow<List<Device>>) : MtpDeviceRepository {
    var observed = 0
    override fun status(): MtpStatus = MtpStatus.Available
    override fun observeDevices(): Flow<List<Device>> = devices.also { observed++ }
}
