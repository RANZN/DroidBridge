package com.ranjan.droidbridge.di

import com.ranjan.droidbridge.domain.repository.DeviceRepository
import com.ranjan.droidbridge.domain.repository.FileRepository
import com.ranjan.droidbridge.domain.repository.SettingsRepository
import com.ranjan.droidbridge.domain.repository.TransferRepository
import com.ranjan.droidbridge.domain.usecase.GetAdbStatusUseCase
import com.ranjan.droidbridge.domain.usecase.ListRemoteFilesUseCase
import com.ranjan.droidbridge.domain.usecase.PushFilesUseCase
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.Test

class AppModuleTest {
    @Test
    fun `dependency graph resolves`() {
        val koin = startKoin { modules(appModules) }.koin
        try {
            koin.get<DeviceRepository>()
            koin.get<FileRepository>()
            koin.get<TransferRepository>()
            koin.get<SettingsRepository>()
            koin.get<GetAdbStatusUseCase>()
            koin.get<ListRemoteFilesUseCase>()
            koin.get<PushFilesUseCase>()
            koin.get<com.ranjan.droidbridge.domain.repository.MtpDeviceRepository>()
            koin.get<com.ranjan.droidbridge.domain.usecase.CheckForUpdateUseCase>()
            koin.get<com.ranjan.droidbridge.domain.usecase.ObserveDevicesUseCase>()
            koin.get<com.ranjan.droidbridge.domain.repository.UpdateRepository>()
        } finally {
            stopKoin()
        }
    }
}
