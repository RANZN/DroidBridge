package com.ranjan.droidbridge.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import com.ranjan.droidbridge.resources.Res
import com.ranjan.droidbridge.resources.droidbridge_icon
import org.jetbrains.compose.resources.painterResource
import java.awt.Taskbar

/** The DroidBridge icon (window icon and Dock). */
@Composable
fun appIcon(): Painter = painterResource(Res.drawable.droidbridge_icon)

/**
 * Shows the icon in the macOS Dock. A packaged .app uses its .icns instead; this covers
 * running from the IDE or `./kotlin run`, where the Dock would show a generic Java icon.
 */
@Composable
fun DockIconEffect(icon: Painter) {
    val density = LocalDensity.current
    LaunchedEffect(icon) {
        if (!Taskbar.isTaskbarSupported()) return@LaunchedEffect
        val taskbar = Taskbar.getTaskbar()
        if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
            runCatching { taskbar.iconImage = icon.toAwtImage(density, LayoutDirection.Ltr, Size(512f, 512f)) }
        }
    }
}
