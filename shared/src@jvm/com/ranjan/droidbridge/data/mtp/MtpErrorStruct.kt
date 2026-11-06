@file:Suppress("FunctionName", "PropertyName", "LocalVariableName")

package com.ranjan.droidbridge.data.mtp

import com.sun.jna.Pointer
import com.sun.jna.Structure

/** LIBMTP_error_t */
@Structure.FieldOrder("errornumber", "error_text", "next")
class MtpErrorStruct : Structure {
    @JvmField var errornumber: Int = 0
    @JvmField var error_text: Pointer? = null
    @JvmField var next: Pointer? = null

    constructor() : super()
    constructor(p: Pointer) : super(p) { read() }
}
