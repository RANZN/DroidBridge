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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ranjan.adbsend.domain.model.FileViewMode
import com.ranjan.adbsend.domain.model.RemoteFile
import com.ranjan.adbsend.domain.usecase.RemotePaths
import com.ranjan.adbsend.presentation.main.MainIntent
import com.ranjan.adbsend.presentation.main.MainState
import com.ranjan.adbsend.presentation.main.SelectionMode
import com.ranjan.adbsend.ui.StagedFilesTransferable
import com.ranjan.adbsend.ui.formatDate
import com.ranjan.adbsend.ui.formatSize
import com.ranjan.adbsend.ui.pickDirectory
import com.ranjan.adbsend.ui.pickFiles
import com.ranjan.adbsend.presentation.main.DragOutRequest
import java.io.File
import java.net.URI

/**
 * Device file browser (list or grid).
 * - Drop files/folders from Finder anywhere on it -> adb push into the current folder.
 * - Drag items out to Finder -> adb pull (staged to a temp folder, then copied by Finder).
 * - Right-click (or Ctrl-click) an item or empty space for more options.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FileBrowser(
    state: MainState,
    onIntent: (MainIntent) -> Unit,
    prepareDragOut: (List<RemoteFile>) -> DragOutRequest,
    modifier: Modifier = Modifier,
) {
    var isDropHovering by remember { mutableStateOf(false) }
    // True while a drag that started in this window is in flight, so we never push our own files back.
    var isDraggingOut by remember { mutableStateOf(false) }
    // Set by an item when it is pressed; the empty-area handler (which runs after the item's) checks it.
    var itemPressed by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    // Keep the keyboard cursor visible.
    LaunchedEffect(state.focusedPath, state.viewMode) {
        val index = state.files.indexOfFirst { it.path == state.focusedPath }
        if (index < 0) return@LaunchedEffect
        if (state.viewMode == FileViewMode.Grid) {
            val visible = gridState.layoutInfo.visibleItemsInfo.map { it.index }
            if (index !in visible.drop(1).dropLast(1)) gridState.scrollToItemIfNeeded(index, visible)
        } else {
            val listIndex = index + 1 // header row
            val visible = listState.layoutInfo.visibleItemsInfo.map { it.index }
            if (listIndex !in visible.drop(1).dropLast(1)) listState.scrollToItemIfNeeded(listIndex, visible)
        }
    }

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

    val itemDragData: (RemoteFile, Boolean) -> DragAndDropTransferData = { file, selected ->
        val files = if (selected) state.selectedFiles else listOf(file)
        isDraggingOut = true
        val request = prepareDragOut(files)
        DragAndDropTransferData(
            transferable = DragAndDropTransferable(StagedFilesTransferable(request)),
            supportedActions = listOf(DragAndDropTransferAction.Copy),
            onTransferCompleted = { action ->
                isDraggingOut = false
                // null = cancelled or dropped back on this window (our drop target refuses it).
                onIntent(MainIntent.DragOutFinished(request, delivered = action != null))
            },
        )
    }

    Column(modifier.fillMaxSize()) {
        Toolbar(state, onIntent)
        HorizontalDivider()
        if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth()) else Spacer(Modifier.size(4.dp))

        ContextMenuHost(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    val columns = if (state.viewMode == FileViewMode.Grid) gridState.layoutInfo.maxSpan else 1
                    val intent = fileBrowserKeyIntent(event, state, columns) ?: return@onPreviewKeyEvent false
                    onIntent(intent)
                    true
                }
                .onPointerEvent(PointerEventType.Press) { event ->
                    runCatching { focusRequester.requestFocus() }
                    val isPlainClick = event.buttons.isPrimaryPressed && !event.keyboardModifiers.isCtrlPressed
                    if (isPlainClick && !itemPressed) onIntent(MainIntent.ClearSelection)
                    itemPressed = false
                }
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { !isDraggingOut && it.dragData() is DragData.FilesList },
                    target = dropTarget,
                ),
            menu = { BackgroundMenu(state, onIntent) },
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
                state.viewMode == FileViewMode.Grid -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(116.dp),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.files, key = { it.path }) { file ->
                        FileItem(file, state, onIntent, itemDragData, onPressed = { itemPressed = true }) { selected -> GridCell(file, selected) }
                    }
                }
                else -> LazyColumn(Modifier.fillMaxSize(), state = listState) {
                    item { HeaderRow() }
                    items(state.files, key = { it.path }) { file ->
                        FileItem(file, state, onIntent, itemDragData, onPressed = { itemPressed = true }) { selected -> ListRow(file, selected) }
                    }
                }
            }

            if (isDropHovering) DropOverlay(state.currentPath)
        }
        HintBar()
    }
}

/** Shared behaviour for list rows and grid cells: selection, open, drag out, context menu. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun FileItem(
    file: RemoteFile,
    state: MainState,
    onIntent: (MainIntent) -> Unit,
    dragData: (RemoteFile, Boolean) -> DragAndDropTransferData,
    onPressed: () -> Unit,
    content: @Composable (selected: Boolean) -> Unit,
) {
    val windowInfo = LocalWindowInfo.current
    val selected = file.path in state.selectedPaths
    val focused = file.path == state.focusedPath
    ContextMenuHost(
        modifier = Modifier.onPointerEvent(PointerEventType.Press) { onPressed() },
        // Like Finder: right-clicking an unselected item selects it first.
        onOpen = { if (!selected) onIntent(MainIntent.SelectFile(file.path)) },
        menu = { ItemMenu(state.actionTargets(file), state, onIntent) },
    ) {
        Box(
            Modifier
                .dragAndDropSource { dragData(file, selected) }
                .combinedClickable(
                    onClick = {
                        val modifiers = windowInfo.keyboardModifiers
                        val mode = when {
                            modifiers.isShiftPressed -> SelectionMode.Range
                            modifiers.isMetaPressed -> SelectionMode.Toggle
                            else -> SelectionMode.Replace
                        }
                        onIntent(MainIntent.SelectFile(file.path, mode))
                    },
                    onDoubleClick = {
                        if (file.isDirectory) onIntent(MainIntent.OpenDirectory(file.path))
                    },
                )
                .then(
                    if (focused) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    else Modifier
                )
        ) {
            content(selected)
        }
    }
}

@Composable
private fun ContextMenuScope.ItemMenu(targets: List<RemoteFile>, state: MainState, onIntent: (MainIntent) -> Unit) {
    val single = targets.singleOrNull()
    val paths = targets.map { it.path }
    val countLabel = if (single != null) "" else " ${targets.size} items"

    if (single?.isDirectory == true) {
        MenuItem("Open", Icons.Default.FolderOpen) { onIntent(MainIntent.OpenDirectory(single.path)) }
        MenuDivider()
    }
    MenuItem("Download$countLabel to ${File(state.downloadDirectory).name}", Icons.Default.Download) {
        onIntent(MainIntent.Download(paths))
    }
    MenuItem("Download$countLabel to...", Icons.Default.SaveAlt) {
        pickDirectory("Download to", state.downloadDirectory)?.let { onIntent(MainIntent.Download(paths, it)) }
    }
    MenuDivider()
    MenuItem("Rename...", Icons.Default.DriveFileRenameOutline, enabled = single != null) {
        single?.let { onIntent(MainIntent.RequestRename(it.path)) }
    }
    MenuItem(if (single != null) "Copy path" else "Copy paths", Icons.Default.ContentCopy) {
        onIntent(MainIntent.CopyPaths(paths))
    }
    MenuDivider()
    MenuItem("Delete$countLabel...", Icons.Default.Delete, destructive = true) {
        onIntent(MainIntent.RequestDelete(paths))
    }
}

@Composable
private fun ContextMenuScope.BackgroundMenu(state: MainState, onIntent: (MainIntent) -> Unit) {
    MenuItem("New folder...", Icons.Default.CreateNewFolder) { onIntent(MainIntent.ShowNewFolderDialog) }
    MenuItem("Upload files...", Icons.Default.Upload) {
        val files = pickFiles("Upload to ${state.currentPath}")
        if (files.isNotEmpty()) onIntent(MainIntent.FilesDropped(files))
    }
    MenuItem("Refresh", Icons.Default.Refresh) { onIntent(MainIntent.Refresh) }
    MenuDivider()
    if (state.viewMode == FileViewMode.List) {
        MenuItem("View as grid", Icons.Default.GridView) { onIntent(MainIntent.SetViewMode(FileViewMode.Grid)) }
    } else {
        MenuItem("View as list", Icons.AutoMirrored.Filled.ViewList) { onIntent(MainIntent.SetViewMode(FileViewMode.List)) }
    }
    MenuItem("Select all", Icons.Default.SelectAll, enabled = state.files.isNotEmpty()) { onIntent(MainIntent.SelectAll) }
    MenuItem("Copy current path", Icons.Default.ContentCopy) { onIntent(MainIntent.CopyPaths(listOf(state.currentPath))) }
}

@Composable
private fun ListRow(file: RemoteFile, selected: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FileIcon(file, Modifier.size(20.dp))
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
private fun GridCell(file: RemoteFile, selected: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FileIcon(file, Modifier.size(52.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            file.name,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.height(34.dp),
        )
        Text(
            if (file.isDirectory) "" else formatSize(file.sizeBytes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FileIcon(file: RemoteFile, modifier: Modifier) {
    Icon(
        if (file.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
        contentDescription = null,
        tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
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
    val selection = state.selectedPaths.toList()
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
            enabled = selection.isNotEmpty(),
        ) { onIntent(MainIntent.Download(selection)) }
        ToolbarButton(Icons.Default.Delete, "Delete selected", enabled = selection.isNotEmpty()) {
            onIntent(MainIntent.RequestDelete(selection))
        }
        VerticalDivider(Modifier.height(24.dp).padding(horizontal = 4.dp))
        ViewModeToggle(Icons.AutoMirrored.Filled.ViewList, "List view", state.viewMode == FileViewMode.List) {
            onIntent(MainIntent.SetViewMode(FileViewMode.List))
        }
        ViewModeToggle(Icons.Default.GridView, "Grid view", state.viewMode == FileViewMode.Grid) {
            onIntent(MainIntent.SetViewMode(FileViewMode.Grid))
        }
        VerticalDivider(Modifier.height(24.dp).padding(horizontal = 4.dp))
        TextButton(onClick = {
            pickDirectory("Choose download folder", state.downloadDirectory)
                ?.let { onIntent(MainIntent.SetDownloadDirectory(it)) }
        }) {
            Text("Save to: ${File(state.downloadDirectory).name}", maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithTooltip(label: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
        content = content,
    )
}

@Composable
private fun ToolbarButton(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) =
    WithTooltip(label) {
        IconButton(onClick = onClick, enabled = enabled) { Icon(icon, contentDescription = label) }
    }

@Composable
private fun ViewModeToggle(icon: ImageVector, label: String, checked: Boolean, onClick: () -> Unit) =
    WithTooltip(label) {
        IconToggleButton(checked = checked, onCheckedChange = { onClick() }) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            "Drag in from Finder to upload  |  Drag out to Finder to download  |  Shift+arrows to select  |  Right-click for more",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private suspend fun androidx.compose.foundation.lazy.LazyListState.scrollToItemIfNeeded(index: Int, visible: List<Int>) {
    val first = visible.firstOrNull() ?: return scrollToItem(index)
    // Moving down: keep the item at the bottom edge; moving up: at the top.
    if (index > first) scrollToItem((index - visible.size + 2).coerceAtLeast(0)) else scrollToItem(index)
}

private suspend fun androidx.compose.foundation.lazy.grid.LazyGridState.scrollToItemIfNeeded(index: Int, visible: List<Int>) {
    val first = visible.firstOrNull() ?: return scrollToItem(index)
    val columns = layoutInfo.maxSpan.coerceAtLeast(1)
    if (index > first) scrollToItem((index - visible.size + 2 * columns).coerceAtLeast(0)) else scrollToItem(index)
}
