package com.ranjan.droidbridge.domain.model

/** Whether MTP support (the libmtp library) is installed on this Mac. */
sealed interface MtpStatus {
    data object Available : MtpStatus
    data class NotInstalled(val installCommand: String) : MtpStatus
}
