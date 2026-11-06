package com.ranjan.droidbridge.domain.model

/** How DroidBridge talks to a device. */
enum class Transport {
    /** Android Debug Bridge: needs USB debugging; full file system, fastest. */
    Adb,

    /** Media Transfer Protocol: the phone's normal "File transfer" USB mode; shared storage only. */
    Mtp,
}
