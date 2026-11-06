package com.ranjan.droidbridge.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import com.ranjan.droidbridge.ui.icons.ContentCopy
import com.ranjan.droidbridge.ui.icons.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ranjan.droidbridge.domain.model.MtpStatus
import com.ranjan.droidbridge.domain.model.Transport

/** A guide step: plain lines, plus an optional extra widget (status, switch, command). */
private class GuideStep(
    val title: String,
    val lines: List<String>,
    val extra: (@Composable (GuideActions) -> Unit)? = null,
)

/** What the guide's interactive bits can do, supplied by the screen. */
class GuideActions(
    val mtpStatus: MtpStatus?,
    val isMtpEnabled: Boolean,
    val onMtpEnabledChange: (Boolean) -> Unit,
    val onCopy: (String) -> Unit,
)

private val adbSteps = listOf(
    GuideStep(
        "Turn on Developer options",
        listOf(
            "On your Android device, open Settings > About phone.",
            "Find \"Build number\" (on some phones it is under Software information).",
            "Tap \"Build number\" 7 times until you see \"You are now a developer\".",
            "Enter your PIN if asked.",
        ),
    ),
    GuideStep(
        "Enable USB debugging",
        listOf(
            "Go back to Settings > System > Developer options.",
            "Switch on \"USB debugging\" and confirm the warning.",
            "Samsung: Settings > Developer options. Xiaomi: also enable \"USB debugging (Security settings)\".",
        ),
    ),
    GuideStep(
        "Connect and allow this Mac",
        listOf(
            "Connect the device to this Mac with a USB cable that supports data (not charge-only).",
            "Unlock the device. A prompt \"Allow USB debugging?\" will appear.",
            "Tick \"Always allow from this computer\" and tap Allow.",
            "If the device shows as \"Unauthorised\" here, unplug it and plug it back in to see the prompt again.",
        ),
    ),
    GuideStep(
        "Optional: connect over Wi-Fi",
        listOf(
            "Android 11+: Developer options > Wireless debugging > Pair device with pairing code.",
            "In Terminal run: adb pair <ip>:<pairing-port> and enter the code.",
            "Then run: adb connect <ip>:<port> (the port shown on the Wireless debugging screen).",
            "The device will appear in the list on the left once connected.",
        ),
    ),
)

private val mtpSteps = listOf(
    GuideStep(
        "Install MTP support on this Mac",
        listOf(
            "macOS cannot talk to Android phones over MTP on its own. DroidBridge uses the free libmtp library.",
            "Install it with Homebrew (https://brew.sh) by running this in Terminal, then restart DroidBridge:",
        ),
        extra = { actions -> MtpInstallStatus(actions) },
    ),
    GuideStep(
        "Turn on MTP in DroidBridge",
        listOf(
            "Switch on \"MTP devices\". You can also do this at the bottom of the device list.",
            "While it is on, DroidBridge uses the phone over MTP, so apps like Image Capture cannot use it at the same time.",
        ),
        extra = { actions ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Switch(checked = actions.isMtpEnabled, onCheckedChange = actions.onMtpEnabledChange)
                Spacer(Modifier.size(12.dp))
                Text(if (actions.isMtpEnabled) "MTP devices are on" else "MTP devices are off")
            }
        },
    ),
    GuideStep(
        "Set the phone to File transfer",
        listOf(
            "Connect the phone with a USB cable that supports data, and unlock it.",
            "Pull down the notification shade and tap \"Charging this device via USB\" (or \"USB options\").",
            "Choose \"File transfer\" (shown as \"Transferring files\" on Samsung, \"File transfer / Android Auto\" on Pixel).",
            "No USB debugging is needed. The phone appears in the device list with an MTP badge.",
        ),
    ),
    GuideStep(
        "If the phone shows as Busy",
        listOf(
            "Another Mac app has taken the phone. Quit Android File Transfer, OpenMTP, Image Capture and Photos.",
            "macOS's camera service can also grab it. Run this in Terminal, then unplug and reconnect the phone:",
        ),
        extra = { actions -> CommandLine("killall ptpcamerad", actions.onCopy) },
    ),
)

@Composable
fun SetupGuide(
    topic: Transport,
    step: Int,
    actions: GuideActions,
    onTopicChange: (Transport) -> Unit,
    onStepChange: (Int) -> Unit,
    onSkip: (dontShowAgain: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dontShowAgain by remember { mutableStateOf(false) }
    val steps = if (topic == Transport.Adb) adbSteps else mtpSteps
    val current = step.coerceIn(0, steps.lastIndex)
    val guide = steps[current]
    val isLast = current == steps.lastIndex

    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
            Column(Modifier.padding(28.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Connect your Android device",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onSkip(dontShowAgain) }) { Text("Skip") }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = topic == Transport.Adb,
                        onClick = { onTopicChange(Transport.Adb) },
                        label = { Text("USB debugging (ADB)") },
                    )
                    FilterChip(
                        selected = topic == Transport.Mtp,
                        onClick = { onTopicChange(Transport.Mtp) },
                        label = { Text("File transfer (MTP)") },
                    )
                }
                Text(
                    if (topic == Transport.Adb) {
                        "Recommended. Fastest, and gives access to the whole file system. Needs USB debugging."
                    } else {
                        "No developer settings needed, but slower, and only shared storage (photos, downloads, documents) is visible."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Waiting for a device... it will appear automatically once it is connected.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepBadge(current + 1)
                    Spacer(Modifier.size(12.dp))
                    Text(
                        "Step ${current + 1} of ${steps.size}: ${guide.title}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(12.dp))
                guide.lines.forEachIndexed { index, line ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text("${index + 1}.", modifier = Modifier.widthIn(min = 24.dp), color = MaterialTheme.colorScheme.primary)
                        Text(line, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                guide.extra?.invoke(actions)

                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = dontShowAgain, onCheckedChange = { dontShowAgain = it })
                    Text("Don't show this again", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (current > 0) OutlinedButton(onClick = { onStepChange(current - 1) }) { Text("Back") }
                        if (isLast) {
                            Button(onClick = { onSkip(dontShowAgain) }) { Text("Done") }
                        } else {
                            Button(onClick = { onStepChange(current + 1) }) { Text("Next") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MtpInstallStatus(actions: GuideActions) {
    when (val status = actions.mtpStatus) {
        MtpStatus.Available -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(8.dp))
            Text("MTP support is installed.", style = MaterialTheme.typography.bodyMedium)
        }
        is MtpStatus.NotInstalled -> Column {
            CommandLine(status.installCommand, actions.onCopy)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Not installed yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        null -> Unit
    }
}

@Composable
private fun CommandLine(command: String, onCopy: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
            Text(command, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            IconButton(onClick = { onCopy(command) }) { Icon(Icons.Default.ContentCopy, contentDescription = "Copy command") }
        }
    }
}

@Composable
private fun StepBadge(number: Int) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text("$number", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
        }
    }
}
