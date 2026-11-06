package com.ranjan.adbsend.presentation

import com.ranjan.adbsend.domain.model.AdbStatus
import com.ranjan.adbsend.domain.model.Device
import com.ranjan.adbsend.domain.model.DeviceState
import com.ranjan.adbsend.domain.model.FileViewMode
import com.ranjan.adbsend.domain.model.RemoteFile
import com.ranjan.adbsend.domain.model.StagedFile
import com.ranjan.adbsend.domain.model.Transfer
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
import com.ranjan.adbsend.domain.usecase.GetAdbStatusUseCase
import com.ranjan.adbsend.domain.usecase.ListRemoteFilesUseCase
import com.ranjan.adbsend.domain.usecase.ObserveDevicesUseCase
import com.ranjan.adbsend.domain.usecase.ObserveTransfersUseCase
import com.ranjan.adbsend.domain.usecase.PullFilesUseCase
import com.ranjan.adbsend.domain.usecase.PushFilesUseCase
import com.ranjan.adbsend.domain.usecase.RenameRemoteFileUseCase
import com.ranjan.adbsend.domain.usecase.SetupGuidePreferenceUseCase
import com.ranjan.adbsend.domain.usecase.StageFileForDragUseCase
import com.ranjan.adbsend.presentation.main.MainIntent
import com.ranjan.adbsend.presentation.main.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DragOutTest {

    private class FakeTransfers : TransferRepository {
        val staged = mutableListOf<String>()
        val discarded = mutableListOf<StagedFile>()
        override val transfers: StateFlow<List<Transfer>> = MutableStateFlow(emptyList())
        override fun push(serial: String, localPaths: List<String>, remoteDirectory: String) = Unit
        override fun pull(serial: String, remotePaths: List<String>, localDirectory: String) = Unit
        override suspend fun stageForDrag(serial: String, remotePath: String): Result<StagedFile> {
            staged += remotePath
            return Result.success(StagedFile(staged.size.toLong(), "/tmp/staged/${remotePath.substringAfterLast('/')}"))
        }
        override fun discardStaged(files: List<StagedFile>) {
            discarded += files
        }
        override fun clearFinished() = Unit
    }

    private val file = RemoteFile("/sdcard/a.txt", "a.txt", false, 1, 0)
    private val transfers = FakeTransfers()
    private var listDelayMs = 0L

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(): MainViewModel {
        val devices = object : DeviceRepository {
            override fun adbStatus() = AdbStatus.Available("/adb")
            override fun observeDevices() = flowOf(listOf(Device("emu", DeviceState.Online, "Pixel", null)))
        }
        val files = object : FileRepository {
            override suspend fun list(serial: String, directory: String) = runCatching {
                kotlinx.coroutines.delay(listDelayMs)
                listOf(file)
            }
            override suspend fun createDirectory(serial: String, path: String) = Result.success(Unit)
            override suspend fun delete(serial: String, path: String) = Result.success(Unit)
            override suspend fun rename(serial: String, from: String, to: String) = Result.success(Unit)
        }
        val settings = object : SettingsRepository {
            override var isSetupGuideDismissed = false
            override var defaultDownloadDirectory = "/tmp"
            override var fileViewMode = FileViewMode.List
        }
        return MainViewModel(
            GetAdbStatusUseCase(devices), ObserveDevicesUseCase(devices), ListRemoteFilesUseCase(files),
            CreateRemoteFolderUseCase(files), DeleteRemoteFilesUseCase(files), ObserveTransfersUseCase(transfers),
            PushFilesUseCase(transfers), PullFilesUseCase(transfers), StageFileForDragUseCase(transfers),
            DiscardStagedFilesUseCase(transfers), ClearFinishedTransfersUseCase(transfers),
            SetupGuidePreferenceUseCase(settings), DownloadDirectoryUseCase(settings),
            RenameRemoteFileUseCase(files), FileViewModeUseCase(settings),
        )
    }

    @Test
    fun `drag dropped back on the app never pulls anything`() = runTest {
        val vm = viewModel()
        val request = vm.prepareDragOut(listOf(file))
        vm.onIntent(MainIntent.DragOutFinished(request, delivered = false))
        assertFalse(request.isStarted)
        assertTrue(transfers.staged.isEmpty())
        assertTrue(transfers.discarded.isEmpty())
    }

    @Test
    fun `staged files are discarded when the drag is not delivered`() = runTest {
        val vm = viewModel()
        val request = vm.prepareDragOut(listOf(file))
        assertEquals(listOf("/tmp/staged/a.txt"), request.localPaths())
        vm.onIntent(MainIntent.DragOutFinished(request, delivered = false))
        assertEquals(listOf("/tmp/staged/a.txt"), transfers.discarded.map { it.localPath })
    }

    @Test
    fun `delivered drags are kept`() = runTest {
        val vm = viewModel()
        val request = vm.prepareDragOut(listOf(file))
        request.localPaths()
        vm.onIntent(MainIntent.DragOutFinished(request, delivered = true))
        assertEquals(listOf("/sdcard/a.txt"), transfers.staged)
        assertTrue(transfers.discarded.isEmpty())
    }

    @Test
    fun `rapid refreshes do not leave a cancellation error`() = runTest {
        // Queued dispatcher, like the real UI thread: a cancelled job resumes later, not inline.
        Dispatchers.setMain(kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        listDelayMs = 50
        val vm = viewModel()
        advanceUntilIdle()
        // Like several uploads finishing: each refresh is mid-listing when the next one arrives.
        repeat(3) {
            vm.onIntent(MainIntent.Refresh)
            runCurrent()
        }
        advanceUntilIdle()
        assertEquals(null, vm.state.value.error)
        assertEquals(listOf(file), vm.state.value.files)
    }
}
