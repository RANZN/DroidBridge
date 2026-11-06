@file:Suppress("FunctionName", "PropertyName", "LocalVariableName")

package com.ranjan.droidbridge.data.mtp

import com.sun.jna.Pointer
import com.sun.jna.Structure

/** LIBMTP_file_t */
@Structure.FieldOrder("item_id", "parent_id", "storage_id", "filename", "filesize", "modificationdate", "filetype", "next")
class MtpFileStruct : Structure {
    @JvmField var item_id: Int = 0
    @JvmField var parent_id: Int = 0
    @JvmField var storage_id: Int = 0
    @JvmField var filename: Pointer? = null
    @JvmField var filesize: Long = 0
    @JvmField var modificationdate: Long = 0 // time_t (64-bit on macOS)
    @JvmField var filetype: Int = 0
    @JvmField var next: Pointer? = null

    constructor() : super()
    constructor(p: Pointer) : super(p) { read() }
}
