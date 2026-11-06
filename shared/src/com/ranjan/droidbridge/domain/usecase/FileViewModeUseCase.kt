package com.ranjan.droidbridge.domain.usecase

import com.ranjan.droidbridge.domain.model.FileViewMode
import com.ranjan.droidbridge.domain.repository.SettingsRepository

class FileViewModeUseCase(private val repository: SettingsRepository) {
    fun get(): FileViewMode = repository.fileViewMode
    fun set(mode: FileViewMode) {
        repository.fileViewMode = mode
    }
}
