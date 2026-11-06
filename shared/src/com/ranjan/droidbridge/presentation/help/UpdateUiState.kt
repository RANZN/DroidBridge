package com.ranjan.droidbridge.presentation.help

import com.ranjan.droidbridge.domain.model.ReleaseInfo

sealed interface UpdateUiState {
    data object Hidden : UpdateUiState
    data object Checking : UpdateUiState
    data class UpToDate(val version: String) : UpdateUiState
    data class Available(val release: ReleaseInfo, val currentVersion: String) : UpdateUiState
    data class Failed(val message: String) : UpdateUiState
}
