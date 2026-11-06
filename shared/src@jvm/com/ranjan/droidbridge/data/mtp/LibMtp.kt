@file:Suppress("FunctionName", "PropertyName", "LocalVariableName")

package com.ranjan.droidbridge.data.mtp

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference

/**
 * JNA binding to the parts of libmtp (https://libmtp.sourceforge.net) DroidBridge uses.
 * uint32_t values are mapped to Int (same bits); MTP object ids above 2^31 appear negative.
 * libmtp is not thread-safe: only call it from [MtpSession]'s single thread.
 */
interface LibMtpNative : Library {
    fun LIBMTP_Init()
    fun LIBMTP_Detect_Raw_Devices(devices: PointerByReference, numdevs: IntByReference): Int
    fun LIBMTP_Open_Raw_Device_Uncached(rawdevice: Pointer): Pointer?
    fun LIBMTP_Release_Device(device: Pointer)
    fun LIBMTP_Get_Friendlyname(device: Pointer): Pointer?
    fun LIBMTP_Get_Modelname(device: Pointer): Pointer?
    fun LIBMTP_Get_Storage(device: Pointer, sortby: Int): Int
    fun LIBMTP_Get_Files_And_Folders(device: Pointer, storage: Int, parent: Int): Pointer?
    fun LIBMTP_Get_Filemetadata(device: Pointer, fileId: Int): Pointer?
    fun LIBMTP_destroy_file_t(file: Pointer)
    fun LIBMTP_Get_File_To_File(device: Pointer, id: Int, path: String, callback: MtpProgressCallback?, data: Pointer?): Int
    fun LIBMTP_Send_File_From_File(device: Pointer, path: String, filedata: MtpFileStruct, callback: MtpProgressCallback?, data: Pointer?): Int
    fun LIBMTP_Create_Folder(device: Pointer, name: String, parentId: Int, storageId: Int): Int
    fun LIBMTP_Delete_Object(device: Pointer, objectId: Int): Int
    fun LIBMTP_Set_File_Name(device: Pointer, file: Pointer, newName: String): Int
    fun LIBMTP_Get_Filetype_Description(filetype: Int): Pointer?
    fun LIBMTP_Get_Errorstack(device: Pointer): Pointer?
    fun LIBMTP_Clear_Errorstack(device: Pointer)
}

/** int (*LIBMTP_progressfunc_t)(uint64_t sent, uint64_t total, void const *data); non-zero cancels. */
fun interface MtpProgressCallback : Callback {
    fun invoke(sent: Long, total: Long, data: Pointer?): Int
}

/** Parent id meaning "top level of a storage". */
const val MTP_ROOT: Int = -1 // 0xFFFFFFFF

/** LIBMTP_device_entry_t */
@Structure.FieldOrder("vendor", "vendor_id", "product", "product_id", "device_flags")
open class MtpDeviceEntry : Structure() {
    @JvmField var vendor: Pointer? = null
    @JvmField var vendor_id: Short = 0
    @JvmField var product: Pointer? = null
    @JvmField var product_id: Short = 0
    @JvmField var device_flags: Int = 0
}

/** LIBMTP_raw_device_t */
@Structure.FieldOrder("device_entry", "bus_location", "devnum")
class MtpRawDevice : Structure {
    @JvmField var device_entry: MtpDeviceEntry = MtpDeviceEntry()
    @JvmField var bus_location: Int = 0
    @JvmField var devnum: Byte = 0

    constructor() : super()
    constructor(p: Pointer) : super(p) { read() }
}

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

/** LIBMTP_error_t */
@Structure.FieldOrder("errornumber", "error_text", "next")
class MtpErrorStruct : Structure {
    @JvmField var errornumber: Int = 0
    @JvmField var error_text: Pointer? = null
    @JvmField var next: Pointer? = null

    constructor() : super()
    constructor(p: Pointer) : super(p) { read() }
}
