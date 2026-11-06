package com.ranjan.droidbridge.fakes

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
import kotlinx.coroutines.flow.flowOf

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
