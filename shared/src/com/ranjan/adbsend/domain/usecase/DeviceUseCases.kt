package com.ranjan.adbsend.domain.usecase

import com.ranjan.adbsend.domain.model.AdbStatus
import com.ranjan.adbsend.domain.model.Device
import com.ranjan.adbsend.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow

class GetAdbStatusUseCase(private val repository: DeviceRepository) {
    operator fun invoke(): AdbStatus = repository.adbStatus()
}

class ObserveDevicesUseCase(private val repository: DeviceRepository) {
    operator fun invoke(): Flow<List<Device>> = repository.observeDevices()
}
