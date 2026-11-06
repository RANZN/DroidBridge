package com.ranjan.droidbridge.data.mtp

/** MTP devices get ids like "mtp:20-5" (USB bus and device number) so they never clash with adb serials. */
object MtpDeviceIds {
    private const val PREFIX = "mtp:"

    fun of(bus: Int, devnum: Int): String = "$PREFIX$bus-$devnum"

    fun isMtp(serial: String): Boolean = serial.startsWith(PREFIX)
}
