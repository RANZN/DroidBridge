package com.ranjan.droidbridge.data.repository

import com.ranjan.droidbridge.domain.model.FileViewMode
import com.ranjan.droidbridge.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.prefs.Preferences

/** Settings stored in macOS user defaults via java.util.prefs. [prefs] is provided by Koin. */
class PreferencesSettingsRepository(
    private val prefs: Preferences,
) : SettingsRepository {

    override var isSetupGuideDismissed: Boolean
        get() = prefs.getBoolean(KEY_GUIDE_DISMISSED, false)
        set(value) = prefs.putBoolean(KEY_GUIDE_DISMISSED, value)

    override var defaultDownloadDirectory: String
        get() = prefs.get(KEY_DOWNLOAD_DIR, null)
            ?: File(System.getProperty("user.home"), "Downloads").absolutePath
        set(value) = prefs.put(KEY_DOWNLOAD_DIR, value)

    override var fileViewMode: FileViewMode
        get() = prefs.get(KEY_VIEW_MODE, null)
            ?.let { saved -> FileViewMode.entries.firstOrNull { it.name == saved } }
            ?: FileViewMode.List
        set(value) = prefs.put(KEY_VIEW_MODE, value.name)

    private val _mtpEnabled = MutableStateFlow(prefs.getBoolean(KEY_MTP_ENABLED, false))
    override val mtpEnabled: StateFlow<Boolean> = _mtpEnabled.asStateFlow()

    override fun setMtpEnabled(enabled: Boolean) {
        prefs.putBoolean(KEY_MTP_ENABLED, enabled)
        _mtpEnabled.value = enabled
    }

    private companion object {
        const val KEY_MTP_ENABLED = "mtpEnabled"
        const val KEY_VIEW_MODE = "fileViewMode"
        const val KEY_GUIDE_DISMISSED = "setupGuideDismissed"
        const val KEY_DOWNLOAD_DIR = "downloadDirectory"
    }
}
