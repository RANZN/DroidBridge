package com.ranjan.adbsend.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF2E7D5B)

@Composable
fun AdbSendTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Color(0xFF7FD8A8), secondary = Color(0xFF9CCBB4))
    } else {
        lightColorScheme(primary = Green, secondary = Color(0xFF4F6358))
    }
    MaterialTheme(colorScheme = colors, content = content)
}
