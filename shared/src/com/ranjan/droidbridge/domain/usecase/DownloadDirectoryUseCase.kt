package com.ranjan.droidbridge.domain.usecase

import com.ranjan.droidbridge.domain.repository.SettingsRepository

class DownloadDirectoryUseCase(private val repository: SettingsRepository) {
    fun get(): String = repository.defaultDownloadDirectory
    fun set(path: String) {
        repository.defaultDownloadDirectory = path
    }
}
