package com.ranjan.droidbridge.domain.model

/** An Android device (physical or emulator) known to the adb server. */
data class Device(
    val serial: String,
    val state: DeviceState,
    val model: String?,
    val product: String?,
    val transport: Transport = Transport.Adb,
) {
    val displayName: String
        get() = model?.replace('_', ' ') ?: serial

    val isReady: Boolean
        get() = state == DeviceState.Online
}

enum class DeviceState {
    Online,
    Unauthorized,
    Offline,

    /** MTP only: another app (Image Capture, Android File Transfer...) holds the device. */
    Busy,
    Unknown,
}
