package com.ranjan.droidbridge.fakes

import com.ranjan.droidbridge.domain.model.AdbStatus
import com.ranjan.droidbridge.domain.model.Device
import com.ranjan.droidbridge.domain.model.RemoteFile
import com.ranjan.droidbridge.domain.model.StagedFile
import com.ranjan.droidbridge.domain.model.Transfer
import com.ranjan.droidbridge.domain.repository.DeviceRepository
import com.ranjan.droidbridge.domain.repository.FileRepository
import com.ranjan.droidbridge.domain.repository.MtpDeviceRepository
import com.ranjan.droidbridge.domain.repository.TransferRepository
import com.ranjan.droidbridge.domain.usecase.ClearFinishedTransfersUseCase
import com.ranjan.droidbridge.domain.usecase.CreateRemoteFolderUseCase
import com.ranjan.droidbridge.domain.usecase.DeleteRemoteFilesUseCase
import com.ranjan.droidbridge.domain.usecase.DiscardStagedFilesUseCase
import com.ranjan.droidbridge.domain.usecase.DownloadDirectoryUseCase
import com.ranjan.droidbridge.domain.usecase.FileViewModeUseCase
import com.ranjan.droidbridge.domain.usecase.GetAdbStatusUseCase
import com.ranjan.droidbridge.domain.usecase.GetMtpStatusUseCase
import com.ranjan.droidbridge.domain.usecase.ListRemoteFilesUseCase
import com.ranjan.droidbridge.domain.usecase.MtpFallbackPolicy
import com.ranjan.droidbridge.domain.usecase.MtpPreferenceUseCase
import com.ranjan.droidbridge.domain.usecase.ObserveDevicesUseCase
import com.ranjan.droidbridge.domain.usecase.ObserveTransfersUseCase
import com.ranjan.droidbridge.domain.usecase.PullFilesUseCase
import com.ranjan.droidbridge.domain.usecase.PushFilesUseCase
import com.ranjan.droidbridge.domain.usecase.RenameRemoteFileUseCase
import com.ranjan.droidbridge.domain.usecase.SetupGuidePreferenceUseCase
import com.ranjan.droidbridge.domain.usecase.StageFileForDragUseCase
import com.ranjan.droidbridge.presentation.main.MainViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

class FakeAdbDevices(private val devices: Flow<List<Device>>) : DeviceRepository {
    override fun adbStatus() = AdbStatus.Available("/adb")
    override fun observeDevices() = devices
}

class StaticFiles(private val files: List<RemoteFile> = emptyList()) : FileRepository {
    override suspend fun list(serial: String, directory: String) = Result.success(files)
    override suspend fun createDirectory(serial: String, path: String) = Result.success(Unit)
    override suspend fun delete(serial: String, path: String) = Result.success(Unit)
    override suspend fun rename(serial: String, from: String, to: String) = Result.success(Unit)
}

open class NoopTransfers : TransferRepository {
    override val transfers: StateFlow<List<Transfer>> = MutableStateFlow(emptyList())
    override fun push(serial: String, localPaths: List<String>, remoteDirectory: String) = Unit
    override fun pull(serial: String, remotePaths: List<String>, localDirectory: String) = Unit
    override suspend fun stageForDrag(serial: String, remotePath: String): Result<StagedFile> =
        Result.failure(UnsupportedOperationException())
    override fun discardStaged(files: List<StagedFile>) = Unit
    override fun clearFinished() = Unit
}

/** Builds a MainViewModel from fakes, the same way Koin wires the real one. */
fun mainViewModelForTest(
    adb: DeviceRepository = FakeAdbDevices(flowOf(emptyList())),
    mtp: MtpDeviceRepository = FakeMtpDevices(flowOf(emptyList())),
    settings: FakeSettings = FakeSettings(),
    files: FileRepository = StaticFiles(),
    transfers: TransferRepository = NoopTransfers(),
    mtpFallback: MtpFallbackPolicy = MtpFallbackPolicy(),
) = MainViewModel(
    GetAdbStatusUseCase(adb), ObserveDevicesUseCase(adb, mtp, settings), ListRemoteFilesUseCase(files),
    CreateRemoteFolderUseCase(files), DeleteRemoteFilesUseCase(files), ObserveTransfersUseCase(transfers),
    PushFilesUseCase(transfers), PullFilesUseCase(transfers), StageFileForDragUseCase(transfers),
    DiscardStagedFilesUseCase(transfers), ClearFinishedTransfersUseCase(transfers),
    SetupGuidePreferenceUseCase(settings), DownloadDirectoryUseCase(settings),
    RenameRemoteFileUseCase(files), FileViewModeUseCase(settings),
    GetMtpStatusUseCase(mtp), MtpPreferenceUseCase(settings), mtpFallback,
)
