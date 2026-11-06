package com.ranjan.adbsend.presentation.main

import com.ranjan.adbsend.domain.model.AdbStatus
import com.ranjan.adbsend.domain.model.Device
import com.ranjan.adbsend.domain.model.FileViewMode
import com.ranjan.adbsend.domain.model.RemoteFile
import com.ranjan.adbsend.domain.model.Transfer
import com.ranjan.adbsend.domain.usecase.RemotePaths

data class MainState(
    val adbStatus: AdbStatus? = null,
    val devices: List<Device> = emptyList(),
    val selectedSerial: String? = null,
    val currentPath: String = RemotePaths.DEFAULT_ROOT,
    val files: List<RemoteFile> = emptyList(),
    val selectedPaths: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val transfers: List<Transfer> = emptyList(),
    val downloadDirectory: String = "",
    val isGuideDismissed: Boolean = false,
    val isGuideRequested: Boolean = false,
    val guideStep: Int = 0,
    val isNewFolderDialogVisible: Boolean = false,
    val pendingDelete: List<RemoteFile> = emptyList(),
    val renameTarget: RemoteFile? = null,
    val viewMode: FileViewMode = FileViewMode.List,
) {
    val selectedDevice: Device? get() = devices.firstOrNull { it.serial == selectedSerial }
    val hasReadyDevice: Boolean get() = devices.any { it.isReady }

    /** Show the setup guide when nothing usable is connected, unless the user skipped it. */
    val showSetupGuide: Boolean
        get() = adbStatus is AdbStatus.Available &&
            (isGuideRequested || (!hasReadyDevice && !isGuideDismissed))

    val selectedFiles: List<RemoteFile> get() = files.filter { it.path in selectedPaths }

    /**
     * Items a context-menu action applies to: the whole selection when the clicked item is part
     * of it (Finder behaviour), otherwise just the clicked item.
     */
    fun actionTargets(clicked: RemoteFile): List<RemoteFile> =
        if (clicked.path in selectedPaths) selectedFiles else listOf(clicked)
    val activeTransferCount: Int get() = transfers.count { !it.isFinished }
}

sealed interface MainIntent {
    // Devices
    data class SelectDevice(val serial: String) : MainIntent

    // Browsing
    data class OpenDirectory(val path: String) : MainIntent
    data object NavigateUp : MainIntent
    data object Refresh : MainIntent
    data class SelectFile(val path: String, val additive: Boolean) : MainIntent
    data object SelectAll : MainIntent
    data object ClearSelection : MainIntent
    data class SetViewMode(val mode: FileViewMode) : MainIntent

    // Transfers
    /** Files/folders dropped from Finder onto the device browser -> adb push. */
    data class FilesDropped(val localPaths: List<String>, val targetDirectory: String? = null) : MainIntent
    /** adb pull [paths] into [directory], or the saved download folder when null. */
    data class Download(val paths: List<String>, val directory: String? = null) : MainIntent
    data class SetDownloadDirectory(val path: String) : MainIntent
    data object ClearFinishedTransfers : MainIntent

    // File management
    data object ShowNewFolderDialog : MainIntent
    data object HideNewFolderDialog : MainIntent
    data class CreateFolder(val name: String) : MainIntent
    data class RequestDelete(val paths: List<String>) : MainIntent
    data object ConfirmDelete : MainIntent
    data object CancelDelete : MainIntent
    data class RequestRename(val path: String) : MainIntent
    data class ConfirmRename(val newName: String) : MainIntent
    data object CancelRename : MainIntent
    data class CopyPaths(val paths: List<String>) : MainIntent

    // Setup guide
    data object OpenSetupGuide : MainIntent
    data class SkipSetupGuide(val dontShowAgain: Boolean) : MainIntent
    data class GoToGuideStep(val step: Int) : MainIntent

    data object DismissError : MainIntent
}

sealed interface MainEffect {
    data class ShowMessage(val message: String) : MainEffect
    data class CopyToClipboard(val text: String) : MainEffect
}
