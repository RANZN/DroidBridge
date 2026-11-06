package com.ranjan.droidbridge.domain.usecase

import com.ranjan.droidbridge.domain.model.MtpStatus
import com.ranjan.droidbridge.domain.repository.MtpDeviceRepository

class GetMtpStatusUseCase(private val repository: MtpDeviceRepository) {
    operator fun invoke(): MtpStatus = repository.status()
}
