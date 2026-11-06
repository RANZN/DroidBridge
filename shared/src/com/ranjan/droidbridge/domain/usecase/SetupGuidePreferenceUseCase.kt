package com.ranjan.droidbridge.domain.usecase

import com.ranjan.droidbridge.domain.repository.SettingsRepository

class SetupGuidePreferenceUseCase(private val repository: SettingsRepository) {
    fun isDismissed(): Boolean = repository.isSetupGuideDismissed
    fun setDismissed(dismissed: Boolean) {
        repository.isSetupGuideDismissed = dismissed
    }
}
