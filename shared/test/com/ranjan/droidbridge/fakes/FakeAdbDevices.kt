package com.ranjan.droidbridge.fakes

import com.ranjan.droidbridge.domain.model.AdbStatus
import com.ranjan.droidbridge.domain.model.Device
import com.ranjan.droidbridge.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow

class FakeAdbDevices(private val devices: Flow<List<Device>>) : DeviceRepository {
    override fun adbStatus() = AdbStatus.Available("/adb")
    override fun observeDevices() = devices
}
