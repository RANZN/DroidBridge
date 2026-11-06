package com.ranjan.adbsend.data.repository

import com.ranjan.adbsend.domain.repository.SettingsRepository
import java.io.File
import java.util.prefs.Preferences

class PreferencesSettingsRepository(
    private val prefs: Preferences = Preferences.userRoot().node("com/ranjan/adbsend"),
) : SettingsRepository {

    override var isSetupGuideDismissed: Boolean
        get() = prefs.getBoolean(KEY_GUIDE_DISMISSED, false)
        set(value) = prefs.putBoolean(KEY_GUIDE_DISMISSED, value)

    override var defaultDownloadDirectory: String
        get() = prefs.get(KEY_DOWNLOAD_DIR, null)
            ?: File(System.getProperty("user.home"), "Downloads").absolutePath
        set(value) = prefs.put(KEY_DOWNLOAD_DIR, value)

    private companion object {
        const val KEY_GUIDE_DISMISSED = "setupGuideDismissed"
        const val KEY_DOWNLOAD_DIR = "downloadDirectory"
    }
}
