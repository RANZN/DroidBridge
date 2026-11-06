package com.ranjan.droidbridge.presentation.help

import com.ranjan.droidbridge.domain.model.AppInfo

data class HelpState(
    val appInfo: AppInfo,
    val isAboutVisible: Boolean = false,
    val update: UpdateUiState = UpdateUiState.Hidden,
)

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
