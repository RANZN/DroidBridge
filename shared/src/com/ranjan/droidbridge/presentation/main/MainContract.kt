package com.ranjan.droidbridge.presentation.main

import com.ranjan.droidbridge.domain.model.AdbStatus
import com.ranjan.droidbridge.domain.model.Device
import com.ranjan.droidbridge.domain.model.FileViewMode
import com.ranjan.droidbridge.domain.model.MtpStatus
import com.ranjan.droidbridge.domain.model.Transport
import com.ranjan.droidbridge.domain.model.RemoteFile
import com.ranjan.droidbridge.domain.model.Transfer
import com.ranjan.droidbridge.domain.usecase.RemotePaths

data class MainState(
    val adbStatus: AdbStatus? = null,
    val mtpStatus: MtpStatus? = null,
    val isMtpEnabled: Boolean = false,
    /** MTP has not produced a usable phone for a while: suggest USB debugging (ADB). */
    val isAdbSuggestionVisible: Boolean = false,
    val devices: List<Device> = emptyList(),
    val selectedSerial: String? = null,
    val currentPath: String = RemotePaths.DEFAULT_ROOT,
    val files: List<RemoteFile> = emptyList(),
    val selectedPaths: Set<String> = emptySet(),
    /** Keyboard cursor; arrow keys move from here. */
    val focusedPath: String? = null,
    /** Start of a Shift range selection. */
    val anchorPath: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val transfers: List<Transfer> = emptyList(),
    val downloadDirectory: String = "",
    val isGuideDismissed: Boolean = false,
    val isGuideRequested: Boolean = false,
    val guideStep: Int = 0,
    /** Which setup guide is showing: USB debugging (ADB) or File transfer (MTP). */
    val guideTopic: Transport = Transport.Adb,
    val isNewFolderDialogVisible: Boolean = false,
    val pendingDelete: List<RemoteFile> = emptyList(),
    val renameTarget: RemoteFile? = null,
    val viewMode: FileViewMode = FileViewMode.List,
) {
    val selectedDevice: Device? get() = devices.firstOrNull { it.serial == selectedSerial }
    val hasReadyDevice: Boolean get() = devices.any { it.isReady }

    /** Show the setup guide when nothing usable is connected, unless the user skipped it. */
    val showSetupGuide: Boolean
        get() = (adbStatus is AdbStatus.Available || isMtpEnabled) &&
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
    data class SelectFile(val path: String, val mode: SelectionMode = SelectionMode.Replace) : MainIntent
    data object SelectAll : MainIntent
    data object ClearSelection : MainIntent

    /** Arrow keys: [delta] is ±1, or ±columns for up/down in grid view. Shift sets [extend]. */
    data class MoveFocus(val delta: Int, val extend: Boolean) : MainIntent
    /** Home / End. */
    data class MoveFocusToEdge(val toEnd: Boolean, val extend: Boolean) : MainIntent
    /** Enter: open the focused folder. */
    data object OpenFocused : MainIntent
    data class SetViewMode(val mode: FileViewMode) : MainIntent

    // Transfers
    /** Files/folders dropped from Finder onto the device browser -> adb push. */
    data class FilesDropped(val localPaths: List<String>, val targetDirectory: String? = null) : MainIntent
    /** adb pull [paths] into [directory], or the saved download folder when null. */
    data class Download(val paths: List<String>, val directory: String? = null) : MainIntent
    data class SetDownloadDirectory(val path: String) : MainIntent
    data object ClearFinishedTransfers : MainIntent
    /**
     * A drag out of the app ended. [delivered] is true only when another app (e.g. Finder)
     * accepted the drop; otherwise any staged copy is discarded and nothing is recorded.
     */
    data class DragOutFinished(val request: DragOutRequest, val delivered: Boolean) : MainIntent

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
    data class CopyText(val text: String) : MainIntent

    // Setup guide
    data class OpenSetupGuide(val topic: Transport = Transport.Adb) : MainIntent
    data class SelectGuideTopic(val topic: Transport) : MainIntent

    // MTP
    data class SetMtpEnabled(val enabled: Boolean) : MainIntent
    /** "Try USB debugging" from the MTP suggestion banner. */
    data object TryAdbInstead : MainIntent
    data object DismissAdbSuggestion : MainIntent
    data class SkipSetupGuide(val dontShowAgain: Boolean) : MainIntent
    data class GoToGuideStep(val step: Int) : MainIntent

    data object DismissError : MainIntent
}

sealed interface MainEffect {
    data class ShowMessage(val message: String) : MainEffect
    data class CopyToClipboard(val text: String) : MainEffect
}
