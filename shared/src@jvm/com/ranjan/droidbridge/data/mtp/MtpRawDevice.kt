@file:Suppress("FunctionName", "PropertyName", "LocalVariableName")

package com.ranjan.droidbridge.data.mtp

import com.sun.jna.Pointer
import com.sun.jna.Structure

/** LIBMTP_raw_device_t */
@Structure.FieldOrder("device_entry", "bus_location", "devnum")
class MtpRawDevice : Structure {
    @JvmField var device_entry: MtpDeviceEntry = MtpDeviceEntry()
    @JvmField var bus_location: Int = 0
    @JvmField var devnum: Byte = 0

    constructor() : super()
    constructor(p: Pointer) : super(p) { read() }
}
