@file:Suppress("FunctionName", "PropertyName", "LocalVariableName")

package com.ranjan.droidbridge.data.mtp

import com.sun.jna.Library
import com.sun.jna.Pointer
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

/** Parent id meaning "top level of a storage". */
const val MTP_ROOT: Int = -1 // 0xFFFFFFFF
