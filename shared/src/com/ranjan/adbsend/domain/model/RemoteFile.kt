package com.ranjan.adbsend.domain.model

/** A file or directory on a device's file system. */
data class RemoteFile(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val modifiedEpochSeconds: Long,
)
