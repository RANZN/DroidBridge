package com.ranjan.adbsend.presentation.main

import androidx.lifecycle.viewModelScope
import com.ranjan.adbsend.domain.model.Device
import com.ranjan.adbsend.domain.model.RemoteFile
import com.ranjan.adbsend.domain.model.Transfer
import com.ranjan.adbsend.domain.model.TransferDirection
import com.ranjan.adbsend.domain.model.TransferStatus
import com.ranjan.adbsend.domain.usecase.ClearFinishedTransfersUseCase
import com.ranjan.adbsend.domain.usecase.CreateRemoteFolderUseCase
import com.ranjan.adbsend.domain.usecase.DeleteRemoteFilesUseCase
import com.ranjan.adbsend.domain.usecase.DownloadDirectoryUseCase
import com.ranjan.adbsend.domain.usecase.GetAdbStatusUseCase
import com.ranjan.adbsend.domain.usecase.ListRemoteFilesUseCase
import com.ranjan.adbsend.domain.usecase.ObserveDevicesUseCase
import com.ranjan.adbsend.domain.usecase.ObserveTransfersUseCase
import com.ranjan.adbsend.domain.usecase.PullFilesUseCase
import com.ranjan.adbsend.domain.usecase.PushFilesUseCase
import com.ranjan.adbsend.domain.usecase.RemotePaths
import com.ranjan.adbsend.domain.usecase.SetupGuidePreferenceUseCase
import com.ranjan.adbsend.domain.usecase.StageFileForDragUseCase
import com.ranjan.adbsend.presentation.mvi.MviViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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
    private val clearFinishedTransfers: ClearFinishedTransfersUseCase,
    private val setupGuidePreference: SetupGuidePreferenceUseCase,
    private val downloadDirectory: DownloadDirectoryUseCase,
) : MviViewModel<MainState, MainIntent, MainEffect>(
    MainState(
        adbStatus = getAdbStatus(),
        isGuideDismissed = setupGuidePreference.isDismissed(),
        downloadDirectory = downloadDirectory.get(),
    )
) {
    private var listJob: Job? = null
    private val reportedTransferIds = mutableSetOf<Long>()

    init {
        viewModelScope.launch { observeDevices().collect(::onDevicesChanged) }
        viewModelScope.launch { observeTransfers().collect(::onTransfersChanged) }
    }

    override fun onIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.SelectDevice -> selectDevice(intent.serial)
            is MainIntent.OpenDirectory -> loadDirectory(intent.path)
            MainIntent.NavigateUp -> RemotePaths.parent(currentState.currentPath)?.let(::loadDirectory)
            MainIntent.Refresh -> loadDirectory(currentState.currentPath, keepSelection = true)
            is MainIntent.SelectFile -> setState {
                val newSelection = when {
                    !intent.additive -> setOf(intent.path)
                    intent.path in selectedPaths -> selectedPaths - intent.path
                    else -> selectedPaths + intent.path
                }
                copy(selectedPaths = newSelection)
            }
            MainIntent.SelectAll -> setState { copy(selectedPaths = files.map { it.path }.toSet()) }
            MainIntent.ClearSelection -> setState { copy(selectedPaths = emptySet()) }

            is MainIntent.FilesDropped -> push(intent.localPaths, intent.targetDirectory)
            MainIntent.DownloadSelected -> downloadSelected()
            is MainIntent.SetDownloadDirectory -> {
                downloadDirectory.set(intent.path)
                setState { copy(downloadDirectory = intent.path) }
            }
            MainIntent.ClearFinishedTransfers -> clearFinishedTransfers()

            MainIntent.ShowNewFolderDialog -> setState { copy(isNewFolderDialogVisible = true) }
            MainIntent.HideNewFolderDialog -> setState { copy(isNewFolderDialogVisible = false) }
            is MainIntent.CreateFolder -> createFolder(intent.name)
            MainIntent.RequestDeleteSelected -> setState { copy(pendingDelete = selectedFiles) }
            MainIntent.CancelDelete -> setState { copy(pendingDelete = emptyList()) }
            MainIntent.ConfirmDelete -> deletePending()

            MainIntent.OpenSetupGuide -> setState { copy(isGuideRequested = true, guideStep = 0) }
            is MainIntent.SkipSetupGuide -> {
                if (intent.dontShowAgain) setupGuidePreference.setDismissed(true)
                setState { copy(isGuideDismissed = true, isGuideRequested = false) }
            }
            is MainIntent.GoToGuideStep -> setState { copy(guideStep = intent.step.coerceAtLeast(0)) }

            MainIntent.DismissError -> setState { copy(error = null) }
        }
    }

    /**
     * Bridge for dragging device files out to Finder. The OS drag system asks for real local
     * files synchronously, so staging (adb pull into a temp folder) starts as soon as the drag
     * begins and the platform layer awaits the result when Finder requests the data.
     */
    fun prepareDragOut(files: List<RemoteFile>): Deferred<List<String>> {
        val serial = currentState.selectedSerial
        // Default dispatcher: the drag system may block the UI thread while awaiting this.
        return viewModelScope.async(Dispatchers.Default) {
            if (serial == null) return@async emptyList()
            files.mapNotNull { file ->
                stageFileForDrag(serial, file.path)
                    .onFailure { sendEffect(MainEffect.ShowMessage("Could not copy ${file.name}: ${it.message}")) }
                    .getOrNull()
            }
        }
    }

    private fun onDevicesChanged(devices: List<Device>) {
        val previous = currentState.selectedDevice
        setState { copy(devices = devices) }
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

    private fun selectDevice(serial: String) {
        setState {
            copy(
                selectedSerial = serial,
                currentPath = RemotePaths.DEFAULT_ROOT,
                files = emptyList(),
                selectedPaths = emptySet(),
                error = null,
            )
        }
        if (currentState.selectedDevice?.isReady == true) loadDirectory(RemotePaths.DEFAULT_ROOT)
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
                files = if (path == currentPath) files else emptyList(),
            )
        }
        listJob = viewModelScope.launch {
            listRemoteFiles(serial, path).fold(
                onSuccess = { files ->
                    setState {
                        copy(
                            files = files,
                            isLoading = false,
                            selectedPaths = selectedPaths.intersect(files.map { it.path }.toSet()),
                        )
                    }
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

    private fun downloadSelected() {
        val serial = currentState.selectedSerial ?: return
        val selected = currentState.selectedFiles
        if (selected.isEmpty()) return
        pullFiles(serial, selected.map { it.path }, currentState.downloadDirectory)
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
