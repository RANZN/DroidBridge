package com.ranjan.droidbridge.ui

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
import com.ranjan.droidbridge.domain.model.AdbStatus
import com.ranjan.droidbridge.domain.model.DeviceState
import com.ranjan.droidbridge.presentation.main.MainEffect
import com.ranjan.droidbridge.presentation.main.MainIntent
import com.ranjan.droidbridge.presentation.main.MainViewModel
import com.ranjan.droidbridge.ui.components.AdbMissingView
import com.ranjan.droidbridge.ui.components.DeleteDialog
import com.ranjan.droidbridge.ui.components.DeviceSidebar
import com.ranjan.droidbridge.ui.components.FileBrowser
import com.ranjan.droidbridge.ui.components.NewFolderDialog
import com.ranjan.droidbridge.ui.components.NoDevicesView
import com.ranjan.droidbridge.ui.components.OfflineView
import com.ranjan.droidbridge.ui.components.RenameDialog
import com.ranjan.droidbridge.ui.components.SetupGuide
import com.ranjan.droidbridge.ui.components.GuideActions
import com.ranjan.droidbridge.ui.components.MtpBusyView
import com.ranjan.droidbridge.ui.components.AdbSuggestionBanner
import com.ranjan.droidbridge.domain.model.Transport
import com.ranjan.droidbridge.ui.components.TransferPanel
import com.ranjan.droidbridge.ui.components.UnauthorizedView
import org.koin.compose.viewmodel.koinViewModel
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun MainScreen(viewModel: MainViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onIntent = viewModel::onIntent
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MainEffect.ShowMessage -> snackbar.showSnackbar(effect.message)
                is MainEffect.CopyToClipboard -> Toolkit.getDefaultToolkit().systemClipboard
                    .setContents(StringSelection(effect.text), null)
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            DeviceSidebar(
                devices = state.devices,
                selectedSerial = state.selectedSerial,
                isMtpEnabled = state.isMtpEnabled,
                mtpStatus = state.mtpStatus,
                onSelect = { onIntent(MainIntent.SelectDevice(it)) },
                onMtpEnabledChange = { onIntent(MainIntent.SetMtpEnabled(it)) },
                onOpenGuide = { onIntent(MainIntent.OpenSetupGuide(it)) },
            )
            VerticalDivider()
            Column(Modifier.weight(1f).fillMaxSize()) {
                if (state.isAdbSuggestionVisible) {
                    AdbSuggestionBanner(
                        onTryAdb = { onIntent(MainIntent.TryAdbInstead) },
                        onDismiss = { onIntent(MainIntent.DismissAdbSuggestion) },
                    )
                }
                val device = state.selectedDevice
                Column(Modifier.weight(1f)) {
                    when {
                        state.adbStatus == AdbStatus.NotFound && !state.isMtpEnabled -> AdbMissingView(
                            onUseMtp = {
                                onIntent(MainIntent.SetMtpEnabled(true))
                                onIntent(MainIntent.OpenSetupGuide(Transport.Mtp))
                            },
                        )
                        state.showSetupGuide -> SetupGuide(
                            topic = state.guideTopic,
                            step = state.guideStep,
                            actions = GuideActions(
                                mtpStatus = state.mtpStatus,
                                isMtpEnabled = state.isMtpEnabled,
                                onMtpEnabledChange = { onIntent(MainIntent.SetMtpEnabled(it)) },
                                onCopy = { onIntent(MainIntent.CopyText(it)) },
                            ),
                            onTopicChange = { onIntent(MainIntent.SelectGuideTopic(it)) },
                            onStepChange = { onIntent(MainIntent.GoToGuideStep(it)) },
                            onSkip = { onIntent(MainIntent.SkipSetupGuide(it)) },
                        )
                        device == null -> NoDevicesView { onIntent(MainIntent.OpenSetupGuide()) }
                        device.state == DeviceState.Unauthorized -> UnauthorizedView(device.displayName)
                        device.state == DeviceState.Busy -> MtpBusyView(device.displayName) {
                            onIntent(MainIntent.OpenSetupGuide(Transport.Mtp))
                        }
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
    state.renameTarget?.let { file ->
        RenameDialog(
            file = file,
            onRename = { onIntent(MainIntent.ConfirmRename(it)) },
            onDismiss = { onIntent(MainIntent.CancelRename) },
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
