package com.ranjan.droidbridge.di

import com.ranjan.droidbridge.data.adb.AdbClient
import com.ranjan.droidbridge.data.adb.AdbLocator
import com.ranjan.droidbridge.data.repository.AdbDeviceRepository
import com.ranjan.droidbridge.data.repository.AdbFileRepository
import com.ranjan.droidbridge.data.mtp.LibMtpDeviceRepository
import com.ranjan.droidbridge.data.mtp.MtpFileRepository
import com.ranjan.droidbridge.data.mtp.MtpLibrary
import com.ranjan.droidbridge.data.mtp.MtpSession
import com.ranjan.droidbridge.data.mtp.MtpTransferEngine
import com.ranjan.droidbridge.data.repository.QueuedTransferRepository
import com.ranjan.droidbridge.data.repository.RoutingFileRepository
import com.ranjan.droidbridge.data.transfer.AdbTransferEngine
import com.ranjan.droidbridge.domain.repository.MtpDeviceRepository
import com.ranjan.droidbridge.data.repository.PreferencesSettingsRepository
import com.ranjan.droidbridge.domain.repository.DeviceRepository
import com.ranjan.droidbridge.domain.repository.FileRepository
import com.ranjan.droidbridge.domain.repository.SettingsRepository
import com.ranjan.droidbridge.domain.repository.TransferRepository
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
import com.ranjan.droidbridge.domain.usecase.SetupGuidePreferenceUseCase
import com.ranjan.droidbridge.domain.usecase.StageFileForDragUseCase
import com.ranjan.droidbridge.presentation.main.MainViewModel
import com.ranjan.droidbridge.presentation.help.HelpViewModel
import com.ranjan.droidbridge.data.update.GitHubUpdateRepository
import com.ranjan.droidbridge.domain.repository.UpdateRepository
import com.ranjan.droidbridge.domain.usecase.CheckForUpdateUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.net.http.HttpClient
import java.time.Duration
import java.util.prefs.Preferences

private val AppScope = named("appScope")

/** java.util.prefs node holding DroidBridge's settings (macOS: ~/Library/Preferences). */
const val PREFERENCES_NODE = "com/ranjan/droidbridge"

val dataModule = module {
    single(AppScope) { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    single { AdbLocator() }
    singleOf(::AdbClient)
    single<DeviceRepository> { AdbDeviceRepository(get()) }
    // MTP (libmtp via JNA)
    single { MtpLibrary() }
    single { MtpSession(get()) }
    single<MtpDeviceRepository> { LibMtpDeviceRepository(get(), get()) }

    // Files and transfers are routed to ADB or MTP by device id
    single<FileRepository> { RoutingFileRepository(adb = AdbFileRepository(get()), mtp = MtpFileRepository(get())) }
    single { QueuedTransferRepository(listOf(AdbTransferEngine(get()), MtpTransferEngine(get())), get(AppScope)) }
    single<TransferRepository> { get<QueuedTransferRepository>() }
    // Platform services: created here and injected, never inside repositories
    single<Preferences> { Preferences.userRoot().node(PREFERENCES_NODE) }
    single<HttpClient> { HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build() }

    singleOf(::PreferencesSettingsRepository) { bind<SettingsRepository>() }
    singleOf(::GitHubUpdateRepository) { bind<UpdateRepository>() }
    single { DroidBridgeAppInfo }
}

val domainModule = module {
    factoryOf(::GetAdbStatusUseCase)
    factoryOf(::ObserveDevicesUseCase)
    factoryOf(::ListRemoteFilesUseCase)
    factoryOf(::CreateRemoteFolderUseCase)
    factoryOf(::DeleteRemoteFilesUseCase)
    factoryOf(::ObserveTransfersUseCase)
    factoryOf(::PushFilesUseCase)
    factoryOf(::PullFilesUseCase)
    factoryOf(::StageFileForDragUseCase)
    factoryOf(::DiscardStagedFilesUseCase)
    factoryOf(::ClearFinishedTransfersUseCase)
    factoryOf(::SetupGuidePreferenceUseCase)
    factoryOf(::DownloadDirectoryUseCase)
    factoryOf(::RenameRemoteFileUseCase)
    factoryOf(::FileViewModeUseCase)
    factoryOf(::GetMtpStatusUseCase)
    factoryOf(::MtpPreferenceUseCase)
    factoryOf(::CheckForUpdateUseCase)
    single { MtpFallbackPolicy() }
}

val presentationModule = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::HelpViewModel)
}

val appModules = listOf(dataModule, domainModule, presentationModule)
