package com.ranjan.droidbridge.domain.usecase

import com.ranjan.droidbridge.domain.model.AdbStatus
import com.ranjan.droidbridge.domain.repository.DeviceRepository

class GetAdbStatusUseCase(private val repository: DeviceRepository) {
    operator fun invoke(): AdbStatus = repository.adbStatus()
}
