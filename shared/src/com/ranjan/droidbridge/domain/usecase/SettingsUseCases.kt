package com.ranjan.droidbridge.domain.usecase

import com.ranjan.droidbridge.domain.model.FileViewMode
import com.ranjan.droidbridge.domain.repository.SettingsRepository

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

class FileViewModeUseCase(private val repository: SettingsRepository) {
    fun get(): FileViewMode = repository.fileViewMode
    fun set(mode: FileViewMode) {
        repository.fileViewMode = mode
    }
}
