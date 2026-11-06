package com.ranjan.adbsend.domain.model

/** Whether the adb binary could be found on this Mac. */
sealed interface AdbStatus {
    data class Available(val path: String) : AdbStatus
    data object NotFound : AdbStatus
}
