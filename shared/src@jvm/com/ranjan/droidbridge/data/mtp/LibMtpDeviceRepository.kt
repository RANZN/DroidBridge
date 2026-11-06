package com.ranjan.droidbridge.data.mtp

import com.ranjan.droidbridge.domain.model.Device
import com.ranjan.droidbridge.domain.model.MtpStatus
import com.ranjan.droidbridge.domain.repository.MtpDeviceRepository
import com.ranjan.droidbridge.domain.util.runSuspendCatching
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class LibMtpDeviceRepository(
    private val session: MtpSession,
    private val library: MtpLibrary,
    private val pollInterval: Duration = 3.seconds,
) : MtpDeviceRepository {

    override fun status(): MtpStatus =
        if (session.isAvailable) MtpStatus.Available else MtpStatus.NotInstalled(library.installCommand)

    override fun observeDevices(): Flow<List<Device>> = flow {
        if (!session.isAvailable) {
            emit(emptyList())
            return@flow
        }
        while (true) {
            emit(runSuspendCatching { session.detect() }.getOrDefault(emptyList()))
            delay(pollInterval)
        }
    }.distinctUntilChanged()
}
