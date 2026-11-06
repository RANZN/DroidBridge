package com.ranjan.droidbridge.presentation.help

import com.ranjan.droidbridge.domain.model.AppInfo
import com.ranjan.droidbridge.domain.model.ReleaseInfo

data class HelpState(
    val appInfo: AppInfo,
    val isAboutVisible: Boolean = false,
    val update: UpdateUiState = UpdateUiState.Hidden,
)

sealed interface UpdateUiState {
    data object Hidden : UpdateUiState
    data object Checking : UpdateUiState
    data class UpToDate(val version: String) : UpdateUiState
    data class Available(val release: ReleaseInfo, val currentVersion: String) : UpdateUiState
    data class Failed(val message: String) : UpdateUiState
}

sealed interface HelpIntent {
    data object ShowAbout : HelpIntent
    data object HideAbout : HelpIntent
    data object CheckForUpdates : HelpIntent
    data object DismissUpdate : HelpIntent
    data class OpenLink(val url: String) : HelpIntent
}

sealed interface HelpEffect {
    data class OpenUrl(val url: String) : HelpEffect
}
