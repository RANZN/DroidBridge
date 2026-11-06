package com.ranjan.droidbridge.domain.usecase

import com.ranjan.droidbridge.domain.model.Transport

/** Pure helpers for POSIX paths on the device. */
object RemotePaths {
    const val DEFAULT_ROOT = "/sdcard"

    /** Where browsing starts: /sdcard over ADB, the storage list ("/") over MTP. */
    fun defaultRoot(transport: Transport): String = when (transport) {
        Transport.Adb -> DEFAULT_ROOT
        Transport.Mtp -> "/"
    }

    val protectedPaths = setOf("/", "/sdcard", "/sdcard/", "/storage", "/storage/emulated/0", "/data", "/system")

    fun child(parent: String, name: String): String =
        if (parent.endsWith("/")) "$parent$name" else "$parent/$name"

    fun parent(path: String): String? {
        val trimmed = path.trimEnd('/')
        if (trimmed.isEmpty()) return null
        val index = trimmed.lastIndexOf('/')
        return if (index <= 0) "/" else trimmed.substring(0, index)
    }

    fun name(path: String): String = path.trimEnd('/').substringAfterLast('/')

    /** Breadcrumb segments: "/sdcard/DCIM" -> ["/", "/sdcard", "/sdcard/DCIM"]. */
    fun breadcrumbs(path: String): List<String> {
        val parts = path.trim('/').split('/').filter { it.isNotEmpty() }
        return listOf("/") + parts.indices.map { i -> "/" + parts.take(i + 1).joinToString("/") }
    }
}
