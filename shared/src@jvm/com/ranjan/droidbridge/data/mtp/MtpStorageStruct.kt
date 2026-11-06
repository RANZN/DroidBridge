@file:Suppress("FunctionName", "PropertyName", "LocalVariableName")

package com.ranjan.droidbridge.data.mtp

import com.sun.jna.Pointer
import com.sun.jna.Structure

/** LIBMTP_devicestorage_t */
@Structure.FieldOrder(
    "id", "StorageType", "FilesystemType", "AccessCapability", "MaxCapacity", "FreeSpaceInBytes",
    "FreeSpaceInObjects", "StorageDescription", "VolumeIdentifier", "next", "prev",
)
class MtpStorageStruct : Structure {
    @JvmField var id: Int = 0
    @JvmField var StorageType: Short = 0
    @JvmField var FilesystemType: Short = 0
    @JvmField var AccessCapability: Short = 0
    @JvmField var MaxCapacity: Long = 0
    @JvmField var FreeSpaceInBytes: Long = 0
    @JvmField var FreeSpaceInObjects: Long = 0
    @JvmField var StorageDescription: Pointer? = null
    @JvmField var VolumeIdentifier: Pointer? = null
    @JvmField var next: Pointer? = null
    @JvmField var prev: Pointer? = null

    constructor() : super()
    constructor(p: Pointer) : super(p) { read() }
}
