package com.ranjan.adbsend.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ranjan.adbsend.domain.model.AdbStatus
import com.ranjan.adbsend.domain.model.DeviceState
import com.ranjan.adbsend.presentation.main.MainEffect
import com.ranjan.adbsend.presentation.main.MainIntent
import com.ranjan.adbsend.presentation.main.MainViewModel
import com.ranjan.adbsend.ui.components.AdbMissingView
import com.ranjan.adbsend.ui.components.DeleteDialog
import com.ranjan.adbsend.ui.components.DeviceSidebar
import com.ranjan.adbsend.ui.components.FileBrowser
import com.ranjan.adbsend.ui.components.NewFolderDialog
import com.ranjan.adbsend.ui.components.NoDevicesView
import com.ranjan.adbsend.ui.components.OfflineView
import com.ranjan.adbsend.ui.components.SetupGuide
import com.ranjan.adbsend.ui.components.TransferPanel
import com.ranjan.adbsend.ui.components.UnauthorizedView
import com.ranjan.adbsend.ui.theme.AdbSendTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdbSendApp() {
    AdbSendTheme { MainScreen() }
}

@Composable
fun MainScreen(viewModel: MainViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onIntent = viewModel::onIntent
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MainEffect.ShowMessage -> snackbar.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            DeviceSidebar(
                devices = state.devices,
                selectedSerial = state.selectedSerial,
                onSelect = { onIntent(MainIntent.SelectDevice(it)) },
                onOpenGuide = { onIntent(MainIntent.OpenSetupGuide) },
            )
            VerticalDivider()
            Column(Modifier.weight(1f).fillMaxSize()) {
                val device = state.selectedDevice
                Column(Modifier.weight(1f)) {
                    when {
                        state.adbStatus == AdbStatus.NotFound -> AdbMissingView()
                        state.showSetupGuide -> SetupGuide(
                            step = state.guideStep,
                            onStepChange = { onIntent(MainIntent.GoToGuideStep(it)) },
                            onSkip = { onIntent(MainIntent.SkipSetupGuide(it)) },
                        )
                        device == null -> NoDevicesView { onIntent(MainIntent.OpenSetupGuide) }
                        device.state == DeviceState.Unauthorized -> UnauthorizedView(device.displayName)
                        !device.isReady -> OfflineView(device.displayName)
                        else -> FileBrowser(state, onIntent, viewModel::prepareDragOut)
                    }
                }
                if (state.transfers.isNotEmpty()) {
                    TransferPanel(state.transfers, onClearFinished = { onIntent(MainIntent.ClearFinishedTransfers) })
                }
            }
        }
    }

    if (state.isNewFolderDialogVisible) {
        NewFolderDialog(
            onCreate = { onIntent(MainIntent.CreateFolder(it)) },
            onDismiss = { onIntent(MainIntent.HideNewFolderDialog) },
        )
    }
    if (state.pendingDelete.isNotEmpty()) {
        DeleteDialog(
            files = state.pendingDelete,
            onConfirm = { onIntent(MainIntent.ConfirmDelete) },
            onDismiss = { onIntent(MainIntent.CancelDelete) },
        )
    }
}
