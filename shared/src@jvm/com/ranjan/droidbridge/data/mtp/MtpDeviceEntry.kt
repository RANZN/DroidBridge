@file:Suppress("FunctionName", "PropertyName", "LocalVariableName")

package com.ranjan.droidbridge.data.mtp

import com.sun.jna.Pointer
import com.sun.jna.Structure

/** LIBMTP_device_entry_t */
@Structure.FieldOrder("vendor", "vendor_id", "product", "product_id", "device_flags")
open class MtpDeviceEntry : Structure() {
    @JvmField var vendor: Pointer? = null
    @JvmField var vendor_id: Short = 0
    @JvmField var product: Pointer? = null
    @JvmField var product_id: Short = 0
    @JvmField var device_flags: Int = 0
}
