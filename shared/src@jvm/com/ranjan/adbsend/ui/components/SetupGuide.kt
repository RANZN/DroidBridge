package com.ranjan.adbsend.ui.components

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class GuideStep(val title: String, val lines: List<String>)

private val guideSteps = listOf(
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

@Composable
fun SetupGuide(
    step: Int,
    onStepChange: (Int) -> Unit,
    onSkip: (dontShowAgain: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dontShowAgain by remember { mutableStateOf(false) }
    val current = step.coerceIn(0, guideSteps.lastIndex)
    val guide = guideSteps[current]
    val isLast = current == guideSteps.lastIndex

    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(Modifier.widthIn(max = 620.dp).fillMaxWidth()) {
            Column(Modifier.padding(28.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Connect your Android device",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onSkip(dontShowAgain) }) { Text("Skip") }
                }
                Text(
                    "Waiting for a device... it will appear automatically once USB debugging is allowed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepBadge(current + 1)
                    Spacer(Modifier.size(12.dp))
                    Text(
                        "Step ${current + 1} of ${guideSteps.size}: ${guide.title}",
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
private fun StepBadge(number: Int) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text("$number", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
        }
    }
}
