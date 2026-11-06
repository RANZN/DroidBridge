package com.ranjan.adbsend.domain.repository

interface SettingsRepository {
    var isSetupGuideDismissed: Boolean
    var defaultDownloadDirectory: String
}
