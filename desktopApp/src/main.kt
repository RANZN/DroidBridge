package com.ranjan.droidbridge

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ranjan.droidbridge.data.mtp.MtpSession
import com.ranjan.droidbridge.data.repository.QueuedTransferRepository
import com.ranjan.droidbridge.di.appModules
import com.ranjan.droidbridge.ui.DroidBridgeWindowContent
import com.ranjan.droidbridge.ui.appIcon
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.context.startKoin

fun main() {
    // Must be set before AWT starts: app name in the macOS menu bar, and use the system menu bar.
    System.setProperty("apple.awt.application.name", "DroidBridge")
    System.setProperty("apple.laf.useScreenMenuBar", "true")

    val koin = startKoin { modules(appModules) }.koin
    application {
        Window(
            onCloseRequest = {
                koin.get<QueuedTransferRepository>().cleanStaging()
                // Hand MTP devices back to macOS so other apps can use them.
                runBlocking { withTimeoutOrNull(2_000) { koin.get<MtpSession>().close() } }
                exitApplication()
            },
            title = "DroidBridge",
            icon = appIcon(),
            state = rememberWindowState(width = 1100.dp, height = 720.dp),
        ) {
            DroidBridgeWindowContent()
        }
    }
}
