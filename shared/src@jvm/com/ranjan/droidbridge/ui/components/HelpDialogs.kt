package com.ranjan.droidbridge.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import com.ranjan.droidbridge.ui.icons.Error
import com.ranjan.droidbridge.ui.icons.NewReleases
import com.ranjan.droidbridge.ui.icons.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ranjan.droidbridge.domain.model.AppInfo
import com.ranjan.droidbridge.presentation.help.UpdateUiState

@Composable
fun AboutDialog(info: AppInfo, onOpenLink: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(40.dp)) },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(info.name)
                Text("Version ${info.version}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "Drag and drop files between your Mac and Android devices, over USB debugging (ADB) or File transfer (MTP).",
                    style = MaterialTheme.typography.bodyMedium,
                )
                HorizontalDivider(Modifier.padding(vertical = 16.dp))
                Text("DEVELOPER", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(info.developerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                info.developerRole?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                    info.developerEmail?.let { email -> TextButton(onClick = { onOpenLink("mailto:$email") }) { Text(email) } }
                    info.developerGithub?.let { user ->
                        TextButton(onClick = { onOpenLink("https://github.com/$user") }) { Text("GitHub (@$user)") }
                    }
                    info.developerWebsite?.let { site -> TextButton(onClick = { onOpenLink(site) }) { Text("Website") } }
                    info.updateRepository?.let { repo ->
                        TextButton(onClick = { onOpenLink("https://github.com/$repo") }) { Text("Source code") }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Built with Kotlin, Compose Multiplatform, adb and libmtp.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
fun UpdateDialog(state: UpdateUiState, onOpenLink: (String) -> Unit, onDismiss: () -> Unit) {
    if (state == UpdateUiState.Hidden) return
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            when (state) {
                is UpdateUiState.Available -> Icon(Icons.Default.NewReleases, contentDescription = null)
                is UpdateUiState.UpToDate -> Icon(Icons.Default.CheckCircle, contentDescription = null)
                is UpdateUiState.Failed -> Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                else -> Unit
            }
        },
        title = {
            Text(
                when (state) {
                    UpdateUiState.Checking -> "Checking for updates"
                    is UpdateUiState.UpToDate -> "You're up to date"
                    is UpdateUiState.Available -> "A new version is available"
                    is UpdateUiState.Failed -> "Couldn't check for updates"
                    UpdateUiState.Hidden -> ""
                }
            )
        },
        text = {
            when (state) {
                UpdateUiState.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Contacting GitHub...")
                }
                is UpdateUiState.UpToDate -> Text("DroidBridge ${state.version} is the latest version.")
                is UpdateUiState.Available -> Column {
                    Text("DroidBridge ${state.release.version} is available. You have ${state.currentVersion}.")
                    state.release.notes?.let { notes ->
                        Spacer(Modifier.height(12.dp))
                        Text("What's new", style = MaterialTheme.typography.titleSmall)
                        Text(
                            notes,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState()),
                        )
                    }
                }
                is UpdateUiState.Failed -> Text(state.message)
                UpdateUiState.Hidden -> Unit
            }
        },
        confirmButton = {
            if (state is UpdateUiState.Available) {
                TextButton(onClick = {
                    onOpenLink(state.release.url)
                    onDismiss()
                }) { Text("Download") }
            } else {
                TextButton(onClick = onDismiss) { Text(if (state == UpdateUiState.Checking) "Cancel" else "OK") }
            }
        },
        dismissButton = if (state is UpdateUiState.Available) ({ TextButton(onClick = onDismiss) { Text("Later") } }) else null,
    )
}
