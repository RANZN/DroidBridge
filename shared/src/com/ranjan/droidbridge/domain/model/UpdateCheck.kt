package com.ranjan.droidbridge.domain.model

sealed interface UpdateCheck {
    data class UpToDate(val currentVersion: String) : UpdateCheck
    data class Available(val release: ReleaseInfo, val currentVersion: String) : UpdateCheck
}
