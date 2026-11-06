package com.ranjan.adbsend.domain.repository

import com.ranjan.adbsend.domain.model.FileViewMode

interface SettingsRepository {
    var isSetupGuideDismissed: Boolean
    var defaultDownloadDirectory: String
    var fileViewMode: FileViewMode
}
