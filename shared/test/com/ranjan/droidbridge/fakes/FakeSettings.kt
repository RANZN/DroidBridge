package com.ranjan.droidbridge.fakes

import com.ranjan.droidbridge.domain.model.FileViewMode
import com.ranjan.droidbridge.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeSettings(mtp: Boolean = false) : SettingsRepository {
    override var isSetupGuideDismissed = false
    override var defaultDownloadDirectory = "/tmp"
    override var fileViewMode = FileViewMode.List
    private val mtpFlow = MutableStateFlow(mtp)
    override val mtpEnabled: StateFlow<Boolean> = mtpFlow
    override fun setMtpEnabled(enabled: Boolean) {
        mtpFlow.value = enabled
    }
}
