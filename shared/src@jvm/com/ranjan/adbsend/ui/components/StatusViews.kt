package com.ranjan.adbsend.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.UsbOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MessageView(
    icon: ImageVector,
    title: String,
    body: String,
    action: (@Composable () -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.widthIn(max = 460.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            action?.invoke()
        }
    }
}

@Composable
fun AdbMissingView() = MessageView(
    icon = Icons.Default.UsbOff,
    title = "adb not found",
    body = "ADBSend needs Android platform-tools. Install them with \"brew install android-platform-tools\" " +
        "or through Android Studio's SDK Manager, then restart ADBSend. You can also set ADB_PATH.",
)

@Composable
fun NoDevicesView(onOpenGuide: () -> Unit) = MessageView(
    icon = Icons.Default.Usb,
    title = "No device connected",
    body = "Connect an Android device with USB debugging turned on. It will appear here automatically.",
    action = { Button(onClick = onOpenGuide) { Text("Show setup guide") } },
)

@Composable
fun UnauthorizedView(deviceName: String) = MessageView(
    icon = Icons.Default.Lock,
    title = "Allow USB debugging on $deviceName",
    body = "Unlock the device and tap Allow on the \"Allow USB debugging?\" prompt. " +
        "If no prompt appears, unplug and reconnect the cable.",
)

@Composable
fun OfflineView(deviceName: String) = MessageView(
    icon = Icons.Default.UsbOff,
    title = "$deviceName is offline",
    body = "Reconnect the cable or restart USB debugging on the device.",
)
