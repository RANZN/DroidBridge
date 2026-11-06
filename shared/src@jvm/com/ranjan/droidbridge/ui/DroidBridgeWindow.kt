package com.ranjan.droidbridge.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ranjan.droidbridge.domain.model.Transport
import com.ranjan.droidbridge.presentation.help.HelpEffect
import com.ranjan.droidbridge.presentation.help.HelpIntent
import com.ranjan.droidbridge.presentation.help.HelpViewModel
import com.ranjan.droidbridge.presentation.main.MainIntent
import com.ranjan.droidbridge.presentation.main.MainViewModel
import com.ranjan.droidbridge.ui.components.AboutDialog
import com.ranjan.droidbridge.ui.components.UpdateDialog
import com.ranjan.droidbridge.ui.theme.DroidBridgeTheme
import org.koin.compose.viewmodel.koinViewModel
import java.awt.Desktop
import java.net.URI

/** Everything inside the main window: the macOS menu bar, the main screen and Help dialogs. */
@Composable
fun FrameWindowScope.DroidBridgeWindowContent(
    mainViewModel: MainViewModel = koinViewModel(),
    helpViewModel: HelpViewModel = koinViewModel(),
) {
    val help by helpViewModel.state.collectAsStateWithLifecycle()
    DockIconEffect(appIcon())

    MenuBar {
        Menu("Help") {
            Item("DroidBridge Help", onClick = { mainViewModel.onIntent(MainIntent.OpenSetupGuide(Transport.Adb)) })
            Item("Connect with File Transfer (MTP)", onClick = { mainViewModel.onIntent(MainIntent.OpenSetupGuide(Transport.Mtp)) })
            Separator()
            Item("Check for Updates...", onClick = { helpViewModel.onIntent(HelpIntent.CheckForUpdates) })
            Item("About the Developer", onClick = { helpViewModel.onIntent(HelpIntent.ShowAbout) })
        }
    }

    // macOS app menu > "About DroidBridge" shows the same dialog.
    DisposableEffect(helpViewModel) {
        val desktop = Desktop.getDesktop().takeIf { Desktop.isDesktopSupported() }
        if (desktop?.isSupported(Desktop.Action.APP_ABOUT) == true) {
            desktop.setAboutHandler { helpViewModel.onIntent(HelpIntent.ShowAbout) }
        }
        onDispose { if (desktop?.isSupported(Desktop.Action.APP_ABOUT) == true) desktop.setAboutHandler(null) }
    }

    LaunchedEffect(helpViewModel) {
        helpViewModel.effects.collect { effect ->
            when (effect) {
                is HelpEffect.OpenUrl -> openLink(effect.url)
            }
        }
    }

    DroidBridgeTheme {
        MainScreen(mainViewModel)
        if (help.isAboutVisible) {
            AboutDialog(
                info = help.appInfo,
                onOpenLink = { helpViewModel.onIntent(HelpIntent.OpenLink(it)) },
                onDismiss = { helpViewModel.onIntent(HelpIntent.HideAbout) },
            )
        }
        UpdateDialog(
            state = help.update,
            onOpenLink = { helpViewModel.onIntent(HelpIntent.OpenLink(it)) },
            onDismiss = { helpViewModel.onIntent(HelpIntent.DismissUpdate) },
        )
    }
}

private fun openLink(url: String) {
    runCatching {
        val desktop = Desktop.getDesktop()
        if (url.startsWith("mailto:")) desktop.mail(URI(url)) else desktop.browse(URI(url))
    }
}
