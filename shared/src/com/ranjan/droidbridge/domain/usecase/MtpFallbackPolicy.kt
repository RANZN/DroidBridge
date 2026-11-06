package com.ranjan.droidbridge.domain.usecase

import com.ranjan.droidbridge.domain.model.Device
import com.ranjan.droidbridge.domain.model.Transport
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * When MTP is switched on but no MTP phone becomes usable within [waitTime] (none connected,
 * or stuck as Busy), DroidBridge suggests USB debugging (ADB) instead.
 */
class MtpFallbackPolicy(val waitTime: Duration = 1.minutes) {
    fun isWaitingForMtp(mtpEnabled: Boolean, devices: List<Device>): Boolean =
        mtpEnabled && devices.none { it.transport == Transport.Mtp && it.isReady }
}
