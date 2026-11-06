package com.ranjan.adbsend.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.UK)
    .withZone(ZoneId.systemDefault())

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes / 1024.0
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return String.format(Locale.UK, "%.1f %s", value, units[unit])
}

fun formatDate(epochSeconds: Long): String =
    if (epochSeconds <= 0) "" else dateFormatter.format(Instant.ofEpochSecond(epochSeconds))
