package com.ranjan.droidbridge.fakes

import com.ranjan.droidbridge.domain.model.Device
import com.ranjan.droidbridge.domain.model.FileViewMode
import com.ranjan.droidbridge.domain.model.MtpStatus
import com.ranjan.droidbridge.domain.repository.MtpDeviceRepository
import com.ranjan.droidbridge.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeSettings(mtp: Boolean = false) : SettingsRepository {
    override var isSetupGuideDismissed = false
    override var defaultDownloadDirectory = "/tmp"
    override var fileViewMode = FileViewMode.List
    private val mtpFlow = MutableStateFlow(mtp)
    override val mtpEnabled: StateFlow<Boolean> = mtpFlow
    override fun setMtpEnabled(enabled: Boolean) {
        mtpFlow.value = enabled
    }
}

class FakeMtpDevices(private val devices: Flow<List<Device>>) : MtpDeviceRepository {
    var observed = 0
    override fun status(): MtpStatus = MtpStatus.Available
    override fun observeDevices(): Flow<List<Device>> = devices.also { observed++ }
}
