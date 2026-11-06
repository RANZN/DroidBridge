package com.ranjan.adbsend.di

import com.ranjan.adbsend.domain.repository.DeviceRepository
import com.ranjan.adbsend.domain.repository.FileRepository
import com.ranjan.adbsend.domain.repository.SettingsRepository
import com.ranjan.adbsend.domain.repository.TransferRepository
import com.ranjan.adbsend.domain.usecase.GetAdbStatusUseCase
import com.ranjan.adbsend.domain.usecase.ListRemoteFilesUseCase
import com.ranjan.adbsend.domain.usecase.PushFilesUseCase
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
        } finally {
            stopKoin()
        }
    }
}
