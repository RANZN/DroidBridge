package com.ranjan.droidbridge.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Scope for building a context menu; [dismiss] closes it. */
class ContextMenuScope(val columnScope: ColumnScope, val dismiss: () -> Unit)

/**
 * Wraps [content] and opens a dropdown at the pointer on right-click (or Ctrl-click, as on macOS).
 * Nested hosts work: the innermost one consumes the click so outer hosts ignore it.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ContextMenuHost(
    modifier: Modifier = Modifier,
    onOpen: () -> Unit = {},
    menu: @Composable ContextMenuScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    var position by remember { mutableStateOf<Offset?>(null) }
    Box(
        modifier.onPointerEvent(PointerEventType.Press) { event ->
            val isContextClick = event.buttons.isSecondaryPressed ||
                (event.buttons.isPrimaryPressed && event.keyboardModifiers.isCtrlPressed)
            val change = event.changes.firstOrNull() ?: return@onPointerEvent
            if (isContextClick && !change.isConsumed) {
                change.consume()
                onOpen()
                position = change.position
            }
        }
    ) {
        content()
        position?.let { pos ->
            Box(Modifier.offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }.size(0.dp)) {
                DropdownMenu(expanded = true, onDismissRequest = { position = null }) {
                    ContextMenuScope(this) { position = null }.menu()
                }
            }
        }
    }
}

@Composable
fun ContextMenuScope.MenuItem(
    label: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val color = if (destructive) MaterialTheme.colorScheme.error else Color.Unspecified
    DropdownMenuItem(
        text = { Text(label, color = if (enabled) color else Color.Unspecified) },
        leadingIcon = icon?.let { { Icon(it, contentDescription = null, tint = if (destructive) color else MaterialTheme.colorScheme.onSurfaceVariant) } },
        enabled = enabled,
        onClick = {
            dismiss()
            onClick()
        },
    )
}

@Composable
fun ContextMenuScope.MenuDivider() = HorizontalDivider()
