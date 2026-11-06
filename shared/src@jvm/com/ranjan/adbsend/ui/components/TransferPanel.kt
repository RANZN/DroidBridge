package com.ranjan.adbsend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ranjan.adbsend.domain.model.Transfer
import com.ranjan.adbsend.domain.model.TransferDirection
import com.ranjan.adbsend.domain.model.TransferStatus
import com.ranjan.adbsend.domain.usecase.RemotePaths
import com.ranjan.adbsend.ui.revealInFinder
import java.io.File

@Composable
fun TransferPanel(
    transfers: List<Transfer>,
    onClearFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        HorizontalDivider()
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            val active = transfers.count { !it.isFinished }
            Text(
                if (active > 0) "Transfers ($active in progress)" else "Transfers",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClearFinished, enabled = transfers.any { it.isFinished }) { Text("Clear finished") }
        }
        LazyColumn(Modifier.heightIn(max = 160.dp)) {
            items(transfers.asReversed(), key = { it.id }) { TransferRow(it) }
        }
    }
}

@Composable
private fun TransferRow(transfer: Transfer) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (transfer.direction == TransferDirection.Push) Icons.Default.Upload else Icons.Default.Download,
            contentDescription = transfer.direction.name,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                RemotePaths.name(transfer.source),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                statusText(transfer),
                style = MaterialTheme.typography.bodySmall,
                color = if (transfer.status is TransferStatus.Failed) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val pulledFile = File(transfer.destination, RemotePaths.name(transfer.source))
        if (transfer.direction == TransferDirection.Pull &&
            transfer.status is TransferStatus.Completed && pulledFile.exists()
        ) {
            TextButton(onClick = { revealInFinder(pulledFile.absolutePath) }) { Text("Show in Finder") }
        }
        StatusIcon(transfer.status)
    }
}

private fun statusText(transfer: Transfer): String {
    val route = "${transfer.deviceSerial}  ->  ${transfer.destination}"
    return when (val status = transfer.status) {
        TransferStatus.Queued -> "Waiting  |  $route"
        TransferStatus.Running -> "Copying  |  $route"
        is TransferStatus.Completed -> status.summary
        is TransferStatus.Failed -> status.reason
    }
}

@Composable
private fun StatusIcon(status: TransferStatus) {
    val modifier = Modifier.size(18.dp)
    when (status) {
        TransferStatus.Queued -> Icon(Icons.Default.Schedule, "Queued", modifier, MaterialTheme.colorScheme.outline)
        TransferStatus.Running -> CircularProgressIndicator(modifier, strokeWidth = 2.dp)
        is TransferStatus.Completed -> Icon(Icons.Default.CheckCircle, "Done", modifier, MaterialTheme.colorScheme.primary)
        is TransferStatus.Failed -> Icon(Icons.Default.Error, "Failed", modifier, MaterialTheme.colorScheme.error)
    }
}
