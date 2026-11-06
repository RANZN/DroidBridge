@file:Suppress("FunctionName", "PropertyName", "LocalVariableName")

package com.ranjan.droidbridge.data.mtp

import com.sun.jna.Pointer
import com.sun.jna.Structure

/** The leading fields of LIBMTP_mtpdevice_t, enough to reach the storage list. */
@Structure.FieldOrder("object_bitsize", "params", "usbinfo", "storage")
class MtpDeviceHead : Structure {
    @JvmField var object_bitsize: Byte = 0
    @JvmField var params: Pointer? = null
    @JvmField var usbinfo: Pointer? = null
    @JvmField var storage: Pointer? = null

    constructor() : super()
    constructor(p: Pointer) : super(p) { read() }
}
