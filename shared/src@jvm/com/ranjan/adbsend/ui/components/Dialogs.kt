package com.ranjan.adbsend.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import com.ranjan.adbsend.domain.model.RemoteFile

@Composable
fun NewFolderDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) =
    NameDialog(title = "New folder", initial = "", confirmLabel = "Create", onConfirm = onCreate, onDismiss = onDismiss)

@Composable
fun RenameDialog(file: RemoteFile, onRename: (String) -> Unit, onDismiss: () -> Unit) =
    NameDialog(title = "Rename", initial = file.name, confirmLabel = "Rename", onConfirm = onRename, onDismiss = onDismiss)

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // Pre-select the name without its extension, as Finder does.
    val stem = initial.substringBeforeLast('.', initial).takeIf { it.isNotEmpty() } ?: initial
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, stem.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val submit = { if (value.text.isNotBlank()) onConfirm(value.text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text("Name") },
                keyboardActions = KeyboardActions(onDone = { submit() }),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = {
            TextButton(onClick = submit, enabled = value.text.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun DeleteDialog(files: List<RemoteFile>, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val label = if (files.size == 1) "\"${files.first().name}\"" else "${files.size} items"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete $label?") },
        text = { Text("This permanently deletes the selected items from the device. It cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
