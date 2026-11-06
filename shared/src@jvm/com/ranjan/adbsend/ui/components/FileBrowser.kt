package com.ranjan.adbsend.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferAction
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.DragAndDropTransferable
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ranjan.adbsend.domain.model.RemoteFile
import com.ranjan.adbsend.domain.usecase.RemotePaths
import com.ranjan.adbsend.presentation.main.MainIntent
import com.ranjan.adbsend.presentation.main.MainState
import com.ranjan.adbsend.ui.StagedFilesTransferable
import com.ranjan.adbsend.ui.formatDate
import com.ranjan.adbsend.ui.formatSize
import com.ranjan.adbsend.ui.pickDirectory
import kotlinx.coroutines.Deferred
import java.io.File
import java.net.URI

/**
 * Device file browser.
 * - Drop files/folders from Finder anywhere on it -> adb push into the current folder.
 * - Drag rows out to Finder -> adb pull (staged to a temp folder, then copied by Finder).
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun FileBrowser(
    state: MainState,
    onIntent: (MainIntent) -> Unit,
    prepareDragOut: (List<RemoteFile>) -> Deferred<List<String>>,
    modifier: Modifier = Modifier,
) {
    var isDropHovering by remember { mutableStateOf(false) }
    // True while a drag that started in this window is in flight, so we never push our own files back.
    var isDraggingOut by remember { mutableStateOf(false) }

    val dropTarget = remember(onIntent) {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) { isDropHovering = true }
            override fun onExited(event: DragAndDropEvent) { isDropHovering = false }
            override fun onEnded(event: DragAndDropEvent) { isDropHovering = false }
            override fun onDrop(event: DragAndDropEvent): Boolean {
                isDropHovering = false
                val files = (event.dragData() as? DragData.FilesList)?.readFiles().orEmpty()
                    .mapNotNull { runCatching { File(URI(it)).absolutePath }.getOrNull() }
                if (files.isEmpty()) return false
                onIntent(MainIntent.FilesDropped(files))
                return true
            }
        }
    }

    Column(modifier.fillMaxSize()) {
        Toolbar(state, onIntent)
        HorizontalDivider()
        if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth()) else Spacer(Modifier.size(4.dp))

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { !isDraggingOut && it.dragData() is DragData.FilesList },
                    target = dropTarget,
                )
        ) {
            when {
                state.error != null -> MessageView(Icons.Default.FolderOpen, "Cannot open folder", state.error) {
                    TextButton(onClick = { onIntent(MainIntent.NavigateUp) }) { Text("Go up") }
                }
                state.files.isEmpty() && !state.isLoading -> MessageView(
                    Icons.Default.FolderOpen,
                    "This folder is empty",
                    "Drag files or folders from Finder here to copy them to the device.",
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    item { HeaderRow() }
                    items(state.files, key = { it.path }) { file ->
                        val selected = file.path in state.selectedPaths
                        FileRow(
                            file = file,
                            selected = selected,
                            onIntent = onIntent,
                            dragData = {
                                val files = if (selected) state.selectedFiles else listOf(file)
                                isDraggingOut = true
                                DragAndDropTransferData(
                                    transferable = DragAndDropTransferable(StagedFilesTransferable(prepareDragOut(files))),
                                    supportedActions = listOf(DragAndDropTransferAction.Copy),
                                    onTransferCompleted = { isDraggingOut = false },
                                )
                            },
                        )
                    }
                }
            }

            if (isDropHovering) DropOverlay(state.currentPath)
        }
        HintBar()
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun FileRow(
    file: RemoteFile,
    selected: Boolean,
    onIntent: (MainIntent) -> Unit,
    dragData: () -> DragAndDropTransferData,
) {
    val windowInfo = LocalWindowInfo.current
    val background = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    Row(
        Modifier
            .fillMaxWidth()
            .background(background)
            .dragAndDropSource { dragData() }
            .combinedClickable(
                onClick = {
                    val modifiers = windowInfo.keyboardModifiers
                    onIntent(MainIntent.SelectFile(file.path, additive = modifiers.isMetaPressed || modifiers.isShiftPressed))
                },
                onDoubleClick = {
                    if (file.isDirectory) onIntent(MainIntent.OpenDirectory(file.path))
                },
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (file.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(file.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            if (file.isDirectory) "--" else formatSize(file.sizeBytes),
            Modifier.width(100.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            formatDate(file.modifiedEpochSeconds),
            Modifier.width(160.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HeaderRow() {
    Row(Modifier.fillMaxWidth().padding(start = 48.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)) {
        val style = MaterialTheme.typography.labelSmall
        val color = MaterialTheme.colorScheme.onSurfaceVariant
        Text("Name", Modifier.weight(1f), style = style, color = color)
        Text("Size", Modifier.width(100.dp), style = style, color = color)
        Text("Modified", Modifier.width(160.dp), style = style, color = color)
    }
}

@Composable
private fun Toolbar(state: MainState, onIntent: (MainIntent) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolbarButton(Icons.Default.ArrowUpward, "Up", enabled = state.currentPath != "/") {
            onIntent(MainIntent.NavigateUp)
        }
        ToolbarButton(Icons.Default.Refresh, "Refresh") { onIntent(MainIntent.Refresh) }

        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemotePaths.breadcrumbs(state.currentPath).forEachIndexed { index, path ->
                if (index > 1) Text("/", color = MaterialTheme.colorScheme.outline)
                val isLast = path.trimEnd('/') == state.currentPath.trimEnd('/').ifEmpty { "/" }
                TextButton(onClick = { onIntent(MainIntent.OpenDirectory(path)) }) {
                    Text(
                        if (path == "/") "/" else RemotePaths.name(path),
                        fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }

        ToolbarButton(Icons.Default.CreateNewFolder, "New folder") { onIntent(MainIntent.ShowNewFolderDialog) }
        ToolbarButton(
            Icons.Default.Download,
            "Download selected to ${state.downloadDirectory}",
            enabled = state.selectedPaths.isNotEmpty(),
        ) { onIntent(MainIntent.DownloadSelected) }
        ToolbarButton(Icons.Default.Delete, "Delete selected", enabled = state.selectedPaths.isNotEmpty()) {
            onIntent(MainIntent.RequestDeleteSelected)
        }
        TextButton(onClick = {
            pickDirectory("Choose download folder", state.downloadDirectory)
                ?.let { onIntent(MainIntent.SetDownloadDirectory(it)) }
        }) {
            Text("Save to: ${File(state.downloadDirectory).name}", maxLines = 1)
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ToolbarButton(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick, enabled = enabled) { Icon(icon, contentDescription = label) }
    }
}

@Composable
private fun DropOverlay(path: String) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Drop to copy to $path",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun HintBar() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            "Drag from Finder to copy to the device  |  Drag files out to Finder to copy to this Mac  |  Cmd-click to select several",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
