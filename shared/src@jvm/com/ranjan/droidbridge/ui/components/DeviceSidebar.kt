package com.ranjan.droidbridge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ranjan.droidbridge.domain.model.Device
import com.ranjan.droidbridge.domain.model.DeviceState
import com.ranjan.droidbridge.domain.model.MtpStatus
import com.ranjan.droidbridge.domain.model.Transport

@Composable
fun DeviceSidebar(
    devices: List<Device>,
    selectedSerial: String?,
    isMtpEnabled: Boolean,
    mtpStatus: MtpStatus?,
    onSelect: (String) -> Unit,
    onMtpEnabledChange: (Boolean) -> Unit,
    onOpenGuide: (Transport) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .width(250.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp)
    ) {
        Text(
            "DEVICES",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 8.dp),
        )
        if (devices.isEmpty()) {
            Text(
                "No devices connected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp),
            )
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(devices, key = { it.serial }) { device ->
                DeviceRow(device, device.serial == selectedSerial) { onSelect(device.serial) }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        MtpToggle(isMtpEnabled, mtpStatus, onMtpEnabledChange, onOpenGuide = { onOpenGuide(Transport.Mtp) })
        TextButton(onClick = { onOpenGuide(Transport.Adb) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("How to connect a device")
        }
    }
}

@Composable
private fun MtpToggle(
    enabled: Boolean,
    status: MtpStatus?,
    onChange: (Boolean) -> Unit,
    onOpenGuide: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("MTP devices", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "Phones in \"File transfer\" mode",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = onChange)
        }
        if (enabled && status is MtpStatus.NotInstalled) {
            Text(
                "MTP support is not installed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
            TextButton(onClick = onOpenGuide, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text("How to set up MTP")
            }
        }
    }
}

@Composable
private fun DeviceRow(device: Device, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                device.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TransportBadge(device.transport)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (device.transport == Transport.Mtp) device.product ?: "USB" else device.serial,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(Modifier.size(8.dp).clip(CircleShape).background(device.state.color()))
    }
}

@Composable
private fun TransportBadge(transport: Transport) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (transport == Transport.Adb) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Text(
            if (transport == Transport.Adb) "ADB" else "MTP",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun DeviceState.color(): Color = when (this) {
    DeviceState.Online -> Color(0xFF34A853)
    DeviceState.Unauthorized, DeviceState.Busy -> Color(0xFFF9AB00)
    DeviceState.Offline, DeviceState.Unknown -> MaterialTheme.colorScheme.outline
}
