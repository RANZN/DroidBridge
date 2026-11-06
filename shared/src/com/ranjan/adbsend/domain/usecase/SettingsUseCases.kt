package com.ranjan.adbsend.domain.usecase

import com.ranjan.adbsend.domain.repository.SettingsRepository

class SetupGuidePreferenceUseCase(private val repository: SettingsRepository) {
    fun isDismissed(): Boolean = repository.isSetupGuideDismissed
    fun setDismissed(dismissed: Boolean) {
        repository.isSetupGuideDismissed = dismissed
    }
}

class DownloadDirectoryUseCase(private val repository: SettingsRepository) {
    fun get(): String = repository.defaultDownloadDirectory
    fun set(path: String) {
        repository.defaultDownloadDirectory = path
    }
}
