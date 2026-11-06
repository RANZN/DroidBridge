package com.ranjan.droidbridge.domain.model

enum class DeviceState {
    Online,
    Unauthorized,
    Offline,

    /** MTP only: another app (Image Capture, Android File Transfer...) holds the device. */
    Busy,
    Unknown,
}
