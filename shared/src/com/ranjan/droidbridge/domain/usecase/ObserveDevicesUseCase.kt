package com.ranjan.droidbridge.domain.usecase

import com.ranjan.droidbridge.domain.model.Device
import com.ranjan.droidbridge.domain.repository.DeviceRepository
import com.ranjan.droidbridge.domain.repository.MtpDeviceRepository
import com.ranjan.droidbridge.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart

/** All devices: ADB devices plus, when MTP is switched on, MTP devices. */
class ObserveDevicesUseCase(
    private val adb: DeviceRepository,
    private val mtp: MtpDeviceRepository,
    private val settings: SettingsRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<Device>> {
        val mtpDevices = settings.mtpEnabled.flatMapLatest { enabled ->
            if (enabled) mtp.observeDevices().onStart { emit(emptyList()) } else flowOf(emptyList())
        }
        return combine(adb.observeDevices().onStart { emit(emptyList()) }, mtpDevices) { a, m -> a + m }
    }
}
