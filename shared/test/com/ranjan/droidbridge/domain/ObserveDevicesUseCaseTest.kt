package com.ranjan.droidbridge.domain

import com.ranjan.droidbridge.domain.model.AdbStatus
import com.ranjan.droidbridge.domain.model.Device
import com.ranjan.droidbridge.domain.model.DeviceState
import com.ranjan.droidbridge.domain.model.Transport
import com.ranjan.droidbridge.domain.repository.DeviceRepository
import com.ranjan.droidbridge.domain.usecase.ObserveDevicesUseCase
import com.ranjan.droidbridge.fakes.FakeMtpDevices
import com.ranjan.droidbridge.fakes.FakeSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveDevicesUseCaseTest {
    private val adbDevice = Device("emulator-5554", DeviceState.Online, "Pixel", null, Transport.Adb)
    private val mtpDevice = Device("mtp:20-5", DeviceState.Online, "Pixel 8", "Google", Transport.Mtp)
    private val adb = object : DeviceRepository {
        override fun adbStatus() = AdbStatus.Available("/adb")
        override fun observeDevices() = flowOf(listOf(adbDevice))
    }

    @Test
    fun `mtp devices are hidden and not polled while mtp is off`() = runTest {
        val mtp = FakeMtpDevices(flowOf(listOf(mtpDevice)))
        val useCase = ObserveDevicesUseCase(adb, mtp, FakeSettings(mtp = false))
        assertEquals(listOf(adbDevice), useCase().first { it.isNotEmpty() })
        assertEquals(0, mtp.observed)
    }

    @Test
    fun `mtp devices are listed after adb devices when mtp is on`() = runTest {
        val mtp = FakeMtpDevices(flowOf(listOf(mtpDevice)))
        val useCase = ObserveDevicesUseCase(adb, mtp, FakeSettings(mtp = true))
        assertEquals(listOf(adbDevice, mtpDevice), useCase().first { it.size == 2 })
    }
}
