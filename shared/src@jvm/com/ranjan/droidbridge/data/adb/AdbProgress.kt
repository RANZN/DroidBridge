package com.ranjan.droidbridge.data.adb

/** One adb progress line: overall [percent] and the item being copied. */
data class AdbProgress(val percent: Int, val item: String)
