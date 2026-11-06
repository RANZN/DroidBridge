package com.ranjan.adbsend

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ranjan.adbsend.data.repository.AdbTransferRepository
import com.ranjan.adbsend.di.appModules
import com.ranjan.adbsend.ui.AdbSendApp
import org.koin.core.context.startKoin

fun main() {
    val koin = startKoin { modules(appModules) }.koin
    application {
        Window(
            onCloseRequest = {
                koin.get<AdbTransferRepository>().cleanStaging()
                exitApplication()
            },
            title = "ADBSend",
            state = rememberWindowState(width = 1100.dp, height = 720.dp),
        ) {
            AdbSendApp()
        }
    }
}
