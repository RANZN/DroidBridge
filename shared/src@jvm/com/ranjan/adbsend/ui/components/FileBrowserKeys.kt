package com.ranjan.adbsend.ui.components

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.ranjan.adbsend.domain.model.FileViewMode
import com.ranjan.adbsend.presentation.main.MainIntent
import com.ranjan.adbsend.presentation.main.MainState

/**
 * Maps a key press in the file browser to an intent (Finder-style shortcuts).
 * Returns null when the key is not handled. [columns] is the grid column count (1 in list view).
 *
 * - Arrows move the cursor; Shift+arrows extend the selection
 * - Home / End jump to first / last (Shift extends)
 * - Enter or Cmd+Down opens the focused folder; Cmd+Up goes to the parent folder
 * - Cmd+A selects all; Esc clears; Cmd+Backspace or Delete asks to delete the selection
 */
fun fileBrowserKeyIntent(event: KeyEvent, state: MainState, columns: Int): MainIntent? {
    if (event.type != KeyEventType.KeyDown) return null
    val shift = event.isShiftPressed
    val cmd = event.isMetaPressed
    val isGrid = state.viewMode == FileViewMode.Grid
    val rowStep = if (isGrid) columns.coerceAtLeast(1) else 1

    return when (event.key) {
        Key.DirectionUp -> if (cmd) MainIntent.NavigateUp else MainIntent.MoveFocus(-rowStep, shift)
        Key.DirectionDown -> if (cmd) MainIntent.OpenFocused else MainIntent.MoveFocus(rowStep, shift)
        Key.DirectionLeft -> if (isGrid) MainIntent.MoveFocus(-1, shift) else null
        Key.DirectionRight -> if (isGrid) MainIntent.MoveFocus(1, shift) else null
        Key.MoveHome -> MainIntent.MoveFocusToEdge(toEnd = false, extend = shift)
        Key.MoveEnd -> MainIntent.MoveFocusToEdge(toEnd = true, extend = shift)
        Key.Enter, Key.NumPadEnter -> MainIntent.OpenFocused
        Key.A -> if (cmd) MainIntent.SelectAll else null
        Key.Escape -> MainIntent.ClearSelection
        Key.Backspace -> if (cmd && state.selectedPaths.isNotEmpty()) MainIntent.RequestDelete(state.selectedPaths.toList()) else null
        Key.Delete -> if (state.selectedPaths.isNotEmpty()) MainIntent.RequestDelete(state.selectedPaths.toList()) else null
        else -> null
    }
}
