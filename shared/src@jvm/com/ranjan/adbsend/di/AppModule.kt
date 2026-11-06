package com.ranjan.adbsend.di

import com.ranjan.adbsend.data.adb.AdbClient
import com.ranjan.adbsend.data.adb.AdbLocator
import com.ranjan.adbsend.data.repository.AdbDeviceRepository
import com.ranjan.adbsend.data.repository.AdbFileRepository
import com.ranjan.adbsend.data.repository.AdbTransferRepository
import com.ranjan.adbsend.data.repository.PreferencesSettingsRepository
import com.ranjan.adbsend.domain.repository.DeviceRepository
import com.ranjan.adbsend.domain.repository.FileRepository
import com.ranjan.adbsend.domain.repository.SettingsRepository
import com.ranjan.adbsend.domain.repository.TransferRepository
import com.ranjan.adbsend.domain.usecase.ClearFinishedTransfersUseCase
import com.ranjan.adbsend.domain.usecase.CreateRemoteFolderUseCase
import com.ranjan.adbsend.domain.usecase.DeleteRemoteFilesUseCase
import com.ranjan.adbsend.domain.usecase.DiscardStagedFilesUseCase
import com.ranjan.adbsend.domain.usecase.DownloadDirectoryUseCase
import com.ranjan.adbsend.domain.usecase.FileViewModeUseCase
import com.ranjan.adbsend.domain.usecase.RenameRemoteFileUseCase
import com.ranjan.adbsend.domain.usecase.GetAdbStatusUseCase
import com.ranjan.adbsend.domain.usecase.ListRemoteFilesUseCase
import com.ranjan.adbsend.domain.usecase.ObserveDevicesUseCase
import com.ranjan.adbsend.domain.usecase.ObserveTransfersUseCase
import com.ranjan.adbsend.domain.usecase.PullFilesUseCase
import com.ranjan.adbsend.domain.usecase.PushFilesUseCase
import com.ranjan.adbsend.domain.usecase.SetupGuidePreferenceUseCase
import com.ranjan.adbsend.domain.usecase.StageFileForDragUseCase
import com.ranjan.adbsend.presentation.main.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val AppScope = named("appScope")

val dataModule = module {
    single(AppScope) { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    single { AdbLocator() }
    singleOf(::AdbClient)
    single<DeviceRepository> { AdbDeviceRepository(get()) }
    singleOf(::AdbFileRepository) { bind<FileRepository>() }
    single { AdbTransferRepository(get(), get(AppScope)) }
    single<TransferRepository> { get<AdbTransferRepository>() }
    single<SettingsRepository> { PreferencesSettingsRepository() }
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
}

val presentationModule = module {
    viewModelOf(::MainViewModel)
}

val appModules = listOf(dataModule, domainModule, presentationModule)
