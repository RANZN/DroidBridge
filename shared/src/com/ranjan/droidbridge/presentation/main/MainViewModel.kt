package com.ranjan.droidbridge.presentation.main

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ensureActive
import com.ranjan.droidbridge.domain.model.Device
import com.ranjan.droidbridge.domain.model.RemoteFile
import com.ranjan.droidbridge.domain.model.StagedFile
import com.ranjan.droidbridge.domain.model.Transfer
import com.ranjan.droidbridge.domain.model.TransferDirection
import com.ranjan.droidbridge.domain.model.TransferStatus
import com.ranjan.droidbridge.domain.model.Transport
import com.ranjan.droidbridge.domain.usecase.ClearFinishedTransfersUseCase
import com.ranjan.droidbridge.domain.usecase.CreateRemoteFolderUseCase
import com.ranjan.droidbridge.domain.usecase.DeleteRemoteFilesUseCase
import com.ranjan.droidbridge.domain.usecase.DiscardStagedFilesUseCase
import com.ranjan.droidbridge.domain.usecase.DownloadDirectoryUseCase
import com.ranjan.droidbridge.domain.usecase.FileViewModeUseCase
import com.ranjan.droidbridge.domain.usecase.RenameRemoteFileUseCase
import com.ranjan.droidbridge.domain.usecase.GetAdbStatusUseCase
import com.ranjan.droidbridge.domain.usecase.GetMtpStatusUseCase
import com.ranjan.droidbridge.domain.usecase.MtpPreferenceUseCase
import com.ranjan.droidbridge.domain.usecase.MtpFallbackPolicy
import com.ranjan.droidbridge.domain.usecase.ListRemoteFilesUseCase
import com.ranjan.droidbridge.domain.usecase.ObserveDevicesUseCase
import com.ranjan.droidbridge.domain.usecase.ObserveTransfersUseCase
import com.ranjan.droidbridge.domain.usecase.PullFilesUseCase
import com.ranjan.droidbridge.domain.usecase.PushFilesUseCase
import com.ranjan.droidbridge.domain.usecase.RemotePaths
import com.ranjan.droidbridge.domain.usecase.SetupGuidePreferenceUseCase
import com.ranjan.droidbridge.domain.usecase.StageFileForDragUseCase
import com.ranjan.droidbridge.presentation.mvi.MviViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel(
    getAdbStatus: GetAdbStatusUseCase,
    private val observeDevices: ObserveDevicesUseCase,
    private val listRemoteFiles: ListRemoteFilesUseCase,
    private val createRemoteFolder: CreateRemoteFolderUseCase,
    private val deleteRemoteFiles: DeleteRemoteFilesUseCase,
    private val observeTransfers: ObserveTransfersUseCase,
    private val pushFiles: PushFilesUseCase,
    private val pullFiles: PullFilesUseCase,
    private val stageFileForDrag: StageFileForDragUseCase,
    private val discardStagedFiles: DiscardStagedFilesUseCase,
    private val clearFinishedTransfers: ClearFinishedTransfersUseCase,
    private val setupGuidePreference: SetupGuidePreferenceUseCase,
    private val downloadDirectory: DownloadDirectoryUseCase,
    private val renameRemoteFile: RenameRemoteFileUseCase,
    private val fileViewMode: FileViewModeUseCase,
    getMtpStatus: GetMtpStatusUseCase,
    private val mtpPreference: MtpPreferenceUseCase,
    private val mtpFallback: MtpFallbackPolicy,
) : MviViewModel<MainState, MainIntent, MainEffect>(
    MainState(
        adbStatus = getAdbStatus(),
        mtpStatus = getMtpStatus(),
        isMtpEnabled = mtpPreference.enabled.value,
        isGuideDismissed = setupGuidePreference.isDismissed(),
        downloadDirectory = downloadDirectory.get(),
        viewMode = fileViewMode.get(),
    )
) {
    private var listJob: Job? = null
    private var mtpFallbackJob: Job? = null
    /** The user dismissed the suggestion; stay quiet until MTP works or is switched off. */
    private var adbSuggestionDismissed = false
    private val reportedTransferIds = mutableSetOf<Long>()

    init {
        viewModelScope.launch { observeDevices().collect(::onDevicesChanged) }
        viewModelScope.launch { observeTransfers().collect(::onTransfersChanged) }
        viewModelScope.launch {
            mtpPreference.enabled.collect {
                setState { copy(isMtpEnabled = it) }
                updateMtpFallback()
            }
        }
    }

    override fun onIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.SelectDevice -> selectDevice(intent.serial)
            is MainIntent.OpenDirectory -> loadDirectory(intent.path)
            MainIntent.NavigateUp -> RemotePaths.parent(currentState.currentPath)?.let(::loadDirectory)
            MainIntent.Refresh -> loadDirectory(currentState.currentPath, keepSelection = true)
            is MainIntent.SelectFile -> setState { SelectionReducer.click(this, intent.path, intent.mode) }
            MainIntent.SelectAll -> setState { SelectionReducer.selectAll(this) }
            MainIntent.ClearSelection -> setState { SelectionReducer.clear(this) }
            is MainIntent.MoveFocus -> setState { SelectionReducer.move(this, intent.delta, intent.extend) }
            is MainIntent.MoveFocusToEdge -> setState { SelectionReducer.edge(this, intent.toEnd, intent.extend) }
            MainIntent.OpenFocused -> currentState.files
                .firstOrNull { it.path == currentState.focusedPath && it.isDirectory }
                ?.let { loadDirectory(it.path) }
            is MainIntent.SetViewMode -> {
                fileViewMode.set(intent.mode)
                setState { copy(viewMode = intent.mode) }
            }

            is MainIntent.FilesDropped -> push(intent.localPaths, intent.targetDirectory)
            is MainIntent.Download -> download(intent.paths, intent.directory)
            is MainIntent.SetDownloadDirectory -> {
                downloadDirectory.set(intent.path)
                setState { copy(downloadDirectory = intent.path) }
            }
            MainIntent.ClearFinishedTransfers -> clearFinishedTransfers()
            is MainIntent.DragOutFinished -> if (!intent.delivered) cancelDragOut(intent.request)

            MainIntent.ShowNewFolderDialog -> setState { copy(isNewFolderDialogVisible = true) }
            MainIntent.HideNewFolderDialog -> setState { copy(isNewFolderDialogVisible = false) }
            is MainIntent.CreateFolder -> createFolder(intent.name)
            is MainIntent.RequestDelete -> setState { copy(pendingDelete = files.filter { it.path in intent.paths }) }
            MainIntent.CancelDelete -> setState { copy(pendingDelete = emptyList()) }
            MainIntent.ConfirmDelete -> deletePending()
            is MainIntent.RequestRename -> setState { copy(renameTarget = files.firstOrNull { it.path == intent.path }) }
            MainIntent.CancelRename -> setState { copy(renameTarget = null) }
            is MainIntent.ConfirmRename -> rename(intent.newName)
            is MainIntent.CopyText -> {
                sendEffect(MainEffect.CopyToClipboard(intent.text))
                sendEffect(MainEffect.ShowMessage("Copied to clipboard"))
            }
            is MainIntent.CopyPaths -> if (intent.paths.isNotEmpty()) {
                sendEffect(MainEffect.CopyToClipboard(intent.paths.joinToString("\n")))
                sendEffect(MainEffect.ShowMessage(if (intent.paths.size == 1) "Path copied" else "${intent.paths.size} paths copied"))
            }

            is MainIntent.OpenSetupGuide -> setState { copy(isGuideRequested = true, guideStep = 0, guideTopic = intent.topic) }
            is MainIntent.SelectGuideTopic -> setState { copy(guideTopic = intent.topic, guideStep = 0) }
            is MainIntent.SetMtpEnabled -> mtpPreference.set(intent.enabled)
            MainIntent.TryAdbInstead -> {
                hideAdbSuggestion()
                setState { copy(isGuideRequested = true, guideStep = 0, guideTopic = Transport.Adb) }
            }
            MainIntent.DismissAdbSuggestion -> hideAdbSuggestion()
            is MainIntent.SkipSetupGuide -> {
                if (intent.dontShowAgain) setupGuidePreference.setDismissed(true)
                setState { copy(isGuideDismissed = true, isGuideRequested = false) }
            }
            is MainIntent.GoToGuideStep -> setState { copy(guideStep = intent.step.coerceAtLeast(0)) }

            MainIntent.DismissError -> setState { copy(error = null) }
        }
    }

    /**
     * Bridge for dragging device files out to Finder. The OS drag system needs real local files
     * synchronously, so this returns a handle that pulls the files only when a drop target asks.
     * The UI reports the end of the drag with [MainIntent.DragOutFinished].
     */
    fun prepareDragOut(files: List<RemoteFile>): DragOutRequest {
        val serial = currentState.selectedSerial
        return DragOutRequest {
            // Default dispatcher: the drag system blocks a UI thread while awaiting this.
            viewModelScope.async(Dispatchers.Default) {
                if (serial == null) return@async emptyList()
                val staged = mutableListOf<StagedFile>()
                try {
                    files.forEach { file ->
                        stageFileForDrag(serial, file.path)
                            .onSuccess { staged += it }
                            .onFailure { sendEffect(MainEffect.ShowMessage("Could not copy ${file.name}: ${it.message}")) }
                    }
                } catch (e: CancellationException) {
                    discardStagedFiles(staged)
                    throw e
                }
                staged
            }
        }
    }

    private fun cancelDragOut(request: DragOutRequest) {
        val staging = request.stagingOrNull ?: return // never started: nothing was pulled
        // Still running: cancelling makes the staging coroutine clean up after itself.
        staging.cancel()
        // Already finished before the cancel: its result survives, so discard it here.
        viewModelScope.launch {
            runCatching { staging.await() }.getOrNull()?.let { discardStagedFiles(it) }
        }
    }

    private fun onDevicesChanged(devices: List<Device>) {
        val previous = currentState.selectedDevice
        setState { copy(devices = devices) }
        updateMtpFallback()
        val stillSelected = devices.firstOrNull { it.serial == previous?.serial }
        when {
            stillSelected?.isReady == true && previous?.isReady == false -> loadDirectory(currentState.currentPath)
            stillSelected != null -> Unit
            else -> {
                val next = devices.firstOrNull { it.isReady } ?: devices.firstOrNull()
                if (next != null) selectDevice(next.serial) else setState {
                    copy(selectedSerial = null, files = emptyList(), selectedPaths = emptySet())
                }
            }
        }
        // A device showing up means the user got through setup; close a guide they opened manually.
        if (devices.any { it.isReady } && previous == null) setState { copy(isGuideRequested = false) }
    }

    /** Starts the MTP timer while MTP is on but unusable; resets everything once it works or is off. */
    private fun updateMtpFallback() {
        val waiting = mtpFallback.isWaitingForMtp(currentState.isMtpEnabled, currentState.devices)
        if (!waiting) {
            mtpFallbackJob?.cancel()
            mtpFallbackJob = null
            adbSuggestionDismissed = false
            if (currentState.isAdbSuggestionVisible) setState { copy(isAdbSuggestionVisible = false) }
            return
        }
        if (mtpFallbackJob?.isActive == true || currentState.isAdbSuggestionVisible || adbSuggestionDismissed) return
        mtpFallbackJob = viewModelScope.launch {
            delay(mtpFallback.waitTime)
            setState { copy(isAdbSuggestionVisible = true) }
        }
    }

    private fun hideAdbSuggestion() {
        adbSuggestionDismissed = true
        setState { copy(isAdbSuggestionVisible = false) }
    }

    private fun selectDevice(serial: String) {
        val root = RemotePaths.defaultRoot(currentState.devices.firstOrNull { it.serial == serial }?.transport ?: Transport.Adb)
        setState {
            copy(
                selectedSerial = serial,
                currentPath = root,
                files = emptyList(),
                selectedPaths = emptySet(),
                focusedPath = null,
                anchorPath = null,
                error = null,
            )
        }
        if (currentState.selectedDevice?.isReady == true) loadDirectory(root)
    }

    private fun loadDirectory(path: String, keepSelection: Boolean = false) {
        val serial = currentState.selectedSerial ?: return
        if (currentState.selectedDevice?.isReady != true) return
        listJob?.cancel()
        setState {
            copy(
                currentPath = path,
                isLoading = true,
                error = null,
                selectedPaths = if (keepSelection) selectedPaths else emptySet(),
                focusedPath = if (keepSelection) focusedPath else null,
                anchorPath = if (keepSelection) anchorPath else null,
                files = if (path == currentPath) files else emptyList(),
            )
        }
        listJob = viewModelScope.launch {
            val result = listRemoteFiles(serial, path)
            ensureActive() // a newer refresh replaced this one: drop the stale result
            result.fold(
                onSuccess = { files ->
                    setState { SelectionReducer.retainExisting(copy(files = files, isLoading = false, error = null)) }
                },
                onFailure = { setState { copy(isLoading = false, files = emptyList(), error = it.message) } },
            )
        }
    }

    private fun push(localPaths: List<String>, targetDirectory: String?) {
        val device = currentState.selectedDevice
        if (device == null || !device.isReady) {
            sendEffect(MainEffect.ShowMessage("Connect and select a device first"))
            return
        }
        pushFiles(device.serial, localPaths, targetDirectory ?: currentState.currentPath)
    }

    private fun download(paths: List<String>, directory: String?) {
        val serial = currentState.selectedSerial ?: return
        if (paths.isEmpty()) return
        pullFiles(serial, paths, directory ?: currentState.downloadDirectory)
    }

    private fun rename(newName: String) {
        val serial = currentState.selectedSerial ?: return
        val target = currentState.renameTarget ?: return
        setState { copy(renameTarget = null) }
        viewModelScope.launch {
            renameRemoteFile(serial, target.path, newName).fold(
                onSuccess = { newPath ->
                    setState { copy(selectedPaths = setOf(newPath), focusedPath = newPath, anchorPath = newPath) }
                    loadDirectory(currentState.currentPath, keepSelection = true)
                },
                onFailure = { sendEffect(MainEffect.ShowMessage("Could not rename ${target.name}: ${it.message}")) },
            )
        }
    }

    private fun createFolder(name: String) {
        val serial = currentState.selectedSerial ?: return
        setState { copy(isNewFolderDialogVisible = false) }
        viewModelScope.launch {
            createRemoteFolder(serial, currentState.currentPath, name).fold(
                onSuccess = { loadDirectory(currentState.currentPath) },
                onFailure = { sendEffect(MainEffect.ShowMessage(it.message ?: "Could not create folder")) },
            )
        }
    }

    private fun deletePending() {
        val serial = currentState.selectedSerial ?: return
        val targets = currentState.pendingDelete
        setState { copy(pendingDelete = emptyList()) }
        viewModelScope.launch {
            deleteRemoteFiles(serial, targets.map { it.path }).onFailure {
                sendEffect(MainEffect.ShowMessage(it.message ?: "Delete failed"))
            }
            loadDirectory(currentState.currentPath)
        }
    }

    private fun onTransfersChanged(transfers: List<Transfer>) {
        setState { copy(transfers = transfers) }
        val newlyFinished = transfers.filter { it.isFinished && reportedTransferIds.add(it.id) }
        newlyFinished.filter { it.status is TransferStatus.Failed }.forEach {
            sendEffect(MainEffect.ShowMessage("Failed: ${RemotePaths.name(it.source)} - ${(it.status as TransferStatus.Failed).reason}"))
        }
        val touchesCurrentDir = newlyFinished.any {
            it.direction == TransferDirection.Push &&
                it.deviceSerial == currentState.selectedSerial &&
                it.destination.trimEnd('/') == currentState.currentPath.trimEnd('/')
        }
        if (touchesCurrentDir) loadDirectory(currentState.currentPath, keepSelection = true)
    }
}
