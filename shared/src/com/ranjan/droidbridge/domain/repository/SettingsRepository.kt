package com.ranjan.droidbridge.domain.repository

import com.ranjan.droidbridge.domain.model.FileViewMode
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    var isSetupGuideDismissed: Boolean
    var defaultDownloadDirectory: String
    var fileViewMode: FileViewMode

    /** MTP is opt-in: it needs libmtp and takes the device away from apps like Image Capture. */
    val mtpEnabled: StateFlow<Boolean>
    fun setMtpEnabled(enabled: Boolean)
}
