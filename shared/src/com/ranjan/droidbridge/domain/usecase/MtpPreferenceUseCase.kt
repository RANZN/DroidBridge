package com.ranjan.droidbridge.domain.usecase

import com.ranjan.droidbridge.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

class MtpPreferenceUseCase(private val settings: SettingsRepository) {
    val enabled: StateFlow<Boolean> get() = settings.mtpEnabled
    fun set(enabled: Boolean) = settings.setMtpEnabled(enabled)
}
