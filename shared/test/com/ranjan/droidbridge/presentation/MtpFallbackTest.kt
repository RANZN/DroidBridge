package com.ranjan.droidbridge.presentation

import com.ranjan.droidbridge.domain.model.Device
import com.ranjan.droidbridge.domain.model.DeviceState
import com.ranjan.droidbridge.domain.model.Transport
import com.ranjan.droidbridge.fakes.FakeMtpDevices
import com.ranjan.droidbridge.fakes.FakeSettings
import com.ranjan.droidbridge.fakes.mainViewModelForTest
import com.ranjan.droidbridge.presentation.main.MainIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class MtpFallbackTest {
    private val busyPhone = Device("mtp:20-5", DeviceState.Busy, "Pixel 8", "Google", Transport.Mtp)
    private val mtpDevices = MutableStateFlow(listOf(busyPhone))
    private val settings = FakeSettings(mtp = true)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel() = mainViewModelForTest(mtp = FakeMtpDevices(mtpDevices), settings = settings).also {
        runCurrent()
    }

    @Test
    fun `suggests adb after one minute without a usable mtp phone`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = viewModel()
        advanceTimeBy(59.seconds)
        assertFalse(vm.state.value.isAdbSuggestionVisible)
        advanceTimeBy(2.seconds)
        assertTrue(vm.state.value.isAdbSuggestionVisible)

        // Try ADB: hides the banner and opens the ADB guide.
        vm.onIntent(MainIntent.TryAdbInstead)
        assertFalse(vm.state.value.isAdbSuggestionVisible)
        assertTrue(vm.state.value.showSetupGuide)
        assertEquals(Transport.Adb, vm.state.value.guideTopic)
    }

    @Test
    fun `no suggestion once the phone works or mtp is switched off`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = viewModel()
        advanceTimeBy(30.seconds)
        mtpDevices.value = listOf(busyPhone.copy(state = DeviceState.Online))
        advanceTimeBy(60.seconds)
        assertFalse(vm.state.value.isAdbSuggestionVisible)

        mtpDevices.value = emptyList() // unplugged: the timer starts again
        advanceTimeBy(61.seconds)
        assertTrue(vm.state.value.isAdbSuggestionVisible)

        vm.onIntent(MainIntent.SetMtpEnabled(false))
        runCurrent()
        assertFalse(vm.state.value.isAdbSuggestionVisible)
    }

    @Test
    fun `dismissed suggestion stays hidden until mtp state changes`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = viewModel()
        advanceTimeBy(61.seconds)
        vm.onIntent(MainIntent.DismissAdbSuggestion)
        advanceTimeBy(300.seconds)
        assertFalse(vm.state.value.isAdbSuggestionVisible)
    }
}
