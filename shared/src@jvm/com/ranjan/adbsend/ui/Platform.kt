package com.ranjan.adbsend.ui

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/** macOS-native folder picker. Returns null if the user cancels. */
fun pickDirectory(title: String, initial: String?): String? {
    System.setProperty("apple.awt.fileDialogForDirectories", "true")
    return try {
        val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
        initial?.let { dialog.directory = it }
        dialog.isVisible = true
        val dir = dialog.directory ?: return null
        val file = dialog.file ?: return null
        File(dir, file).absolutePath
    } finally {
        System.setProperty("apple.awt.fileDialogForDirectories", "false")
    }
}

/** macOS-native multi-file picker. Returns an empty list if the user cancels. */
fun pickFiles(title: String): List<String> {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.isMultipleMode = true
    dialog.isVisible = true
    return dialog.files.orEmpty().map { it.absolutePath }
}

/** Opens Finder with [path] selected. */
fun revealInFinder(path: String) {
    runCatching { ProcessBuilder("open", "-R", path).start() }
}
