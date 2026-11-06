package com.ranjan.droidbridge.data.mtp

import com.ranjan.droidbridge.data.transfer.TransferFailedException
import com.ranjan.droidbridge.data.transfer.TransferProgress
import com.ranjan.droidbridge.domain.model.Device
import com.ranjan.droidbridge.domain.model.DeviceState
import com.ranjan.droidbridge.domain.model.RemoteFile
import com.ranjan.droidbridge.domain.model.Transport
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

/**
 * Owns every libmtp call. libmtp is not thread-safe, so all work runs on one dedicated thread
 * and devices stay open between calls (opening an MTP device takes a second or two).
 *
 * Paths look like "/<storage>/<folder>/<file>"; "/" lists the device's storages.
 * MTP addresses objects by numeric id, so folder listings are cached to map paths to ids.
 */
class MtpSession(private val library: MtpLibrary) {

    private val dispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, "droidbridge-mtp").apply { isDaemon = true }
    }.asCoroutineDispatcher()

    private class Storage(val id: Int, val name: String)

    private class Entry(
        val id: Int,
        val storageId: Int,
        val name: String,
        val isDirectory: Boolean,
        val size: Long,
        val modified: Long,
    )

    private class OpenDevice(
        val serial: String,
        val raw: Memory, // kept alive for the device's lifetime
        val handle: Pointer,
        val name: String,
        val vendor: String?,
        val storages: List<Storage>,
        val listings: MutableMap<Pair<Int, Int>, List<Entry>> = mutableMapOf(),
    )

    private val open = mutableMapOf<String, OpenDevice>()

    /** Devices that could not be opened (usually busy): polls to wait before retrying. */
    private val retryAfter = mutableMapOf<String, Int>()

    private var folderType = 0
    private var unknownType = -1

    val isAvailable: Boolean get() = library.native != null

    private suspend fun <T> onMtpThread(block: suspend CoroutineScope.(LibMtpNative) -> T): T {
        val lib = library.native ?: throw TransferFailedException("MTP support is not installed (${library.installCommand})")
        return withContext(dispatcher) {
            if (unknownType == -1) resolveFiletypes(lib)
            block(lib)
        }
    }

    // ---------------------------------------------------------------- devices

    suspend fun detect(): List<Device> = onMtpThread { lib ->
        val found = detectRaw(lib)
        // Release devices that were unplugged.
        (open.keys - found.map { it.serial }.toSet()).forEach { serial ->
            open.remove(serial)?.let { lib.LIBMTP_Release_Device(it.handle) }
        }
        retryAfter.keys.retainAll(found.map { it.serial }.toSet())

        found.map { raw ->
            val device = open[raw.serial] ?: tryOpen(lib, raw)
            if (device != null) {
                Device(raw.serial, DeviceState.Online, device.name, device.vendor, Transport.Mtp)
            } else {
                Device(raw.serial, DeviceState.Busy, raw.product ?: "Android device", raw.vendor, Transport.Mtp)
            }
        }
    }

    private class Raw(val serial: String, val memory: Memory, val vendor: String?, val product: String?)

    private fun detectRaw(lib: LibMtpNative): List<Raw> {
        val array = PointerByReference()
        val count = IntByReference()
        val rc = lib.LIBMTP_Detect_Raw_Devices(array, count)
        val base = array.value
        try {
            if (rc != 0 || base == null) return emptyList() // 5 = no device attached
            val size = MtpRawDevice().size()
            return (0 until count.value).map { i ->
                val struct = MtpRawDevice(base.share(i.toLong() * size))
                // Copy each entry: libmtp's array is freed below, but the device keeps using it.
                val memory = Memory(size.toLong()).apply { write(0, base.getByteArray(i.toLong() * size, size), 0, size) }
                Raw(
                    serial = MtpDeviceIds.of(struct.bus_location, struct.devnum.toInt() and 0xFF),
                    memory = memory,
                    vendor = struct.device_entry.vendor?.getString(0),
                    product = struct.device_entry.product?.getString(0),
                )
            }
        } finally {
            if (base != null) Native.free(Pointer.nativeValue(base))
        }
    }

    private fun tryOpen(lib: LibMtpNative, raw: Raw): OpenDevice? {
        val wait = retryAfter[raw.serial] ?: 0
        if (wait > 0) {
            retryAfter[raw.serial] = wait - 1
            return null
        }
        val handle = lib.LIBMTP_Open_Raw_Device_Uncached(raw.memory)
        if (handle == null) {
            retryAfter[raw.serial] = BUSY_RETRY_POLLS
            return null
        }
        lib.LIBMTP_Get_Storage(handle, 0)
        val storages = readStorages(handle)
        val name = lib.LIBMTP_Get_Friendlyname(handle).takeString()
            ?: lib.LIBMTP_Get_Modelname(handle).takeString()
            ?: raw.product
            ?: "Android device"
        return OpenDevice(raw.serial, raw.memory, handle, name, raw.vendor, storages).also {
            open[raw.serial] = it
            retryAfter.remove(raw.serial)
        }
    }

    private fun readStorages(handle: Pointer): List<Storage> {
        val structs = buildList {
            var p = MtpDeviceHead(handle).storage
            while (p != null) {
                val s = MtpStorageStruct(p)
                add(s.id to (s.StorageDescription?.getString(0) ?: s.VolumeIdentifier?.getString(0) ?: "Storage"))
                p = s.next
            }
        }
        val names = MtpPaths.uniqueStorageNames(structs.map { it.second })
        return structs.mapIndexed { i, (id, _) -> Storage(id, names[i]) }
    }

    suspend fun close() = withContext(dispatcher) {
        library.native?.let { lib -> open.values.forEach { lib.LIBMTP_Release_Device(it.handle) } }
        open.clear()
    }

    // ---------------------------------------------------------------- files

    suspend fun list(serial: String, path: String): List<RemoteFile> = onMtpThread { lib ->
        val device = device(serial)
        val segments = MtpPaths.segments(path)
        if (segments.isEmpty()) {
            return@onMtpThread device.storages.map { RemoteFile("/${it.name}", it.name, true, 0, 0) }
        }
        val (storageId, folderId) = resolveFolder(lib, device, segments)
        children(lib, device, storageId, folderId, refresh = true).map { entry ->
            RemoteFile(MtpPaths.join(segments + entry.name), entry.name, entry.isDirectory, entry.size, entry.modified)
        }
    }

    suspend fun createFolder(serial: String, path: String) = onMtpThread { lib ->
        val device = device(serial)
        val segments = MtpPaths.segments(path)
        require(segments.size >= 2) { "Choose a storage first" }
        val (storageId, parentId) = resolveFolder(lib, device, segments.dropLast(1))
        val existing = children(lib, device, storageId, parentId).firstOrNull { it.name == segments.last() }
        if (existing?.isDirectory == true) return@onMtpThread
        if (existing != null) error("A file with that name already exists")
        val id = lib.LIBMTP_Create_Folder(device.handle, segments.last(), parentId, storageId)
        device.listings.clear()
        if (id == 0) error(lastError(lib, device, "Could not create folder"))
    }

    suspend fun delete(serial: String, path: String) = onMtpThread { lib ->
        val device = device(serial)
        val entry = lookup(lib, device, MtpPaths.segments(path))
        val rc = lib.LIBMTP_Delete_Object(device.handle, entry.id)
        device.listings.clear()
        if (rc != 0) error(lastError(lib, device, "Could not delete"))
    }

    suspend fun rename(serial: String, from: String, to: String) = onMtpThread { lib ->
        val device = device(serial)
        val fromSegments = MtpPaths.segments(from)
        val toSegments = MtpPaths.segments(to)
        require(fromSegments.dropLast(1) == toSegments.dropLast(1)) { "Can only rename within the same folder" }
        val entry = lookup(lib, device, fromSegments)
        val (storageId, parentId) = resolveFolder(lib, device, fromSegments.dropLast(1))
        if (children(lib, device, storageId, parentId).any { it.name == toSegments.last() }) {
            error("An item with that name already exists")
        }
        val meta = lib.LIBMTP_Get_Filemetadata(device.handle, entry.id) ?: error(lastError(lib, device, "Could not rename"))
        try {
            val rc = lib.LIBMTP_Set_File_Name(device.handle, meta, toSegments.last())
            if (rc != 0) error(lastError(lib, device, "Could not rename"))
        } finally {
            lib.LIBMTP_destroy_file_t(meta)
            device.listings.clear()
        }
    }

    // ---------------------------------------------------------------- transfers

    /** Copies a device file or folder into [localDirectory]. */
    suspend fun download(
        serial: String,
        remotePath: String,
        localDirectory: String,
        onProgress: (TransferProgress) -> Unit,
    ): String = onMtpThread { lib ->
        val device = device(serial)
        val root = lookup(lib, device, MtpPaths.segments(remotePath))
        // Plan the whole copy first so progress covers every file in a folder.
        val plan = mutableListOf<Pair<Entry, String>>()
        fun walk(entry: Entry, relative: String) {
            plan += entry to relative
            if (entry.isDirectory) {
                children(lib, device, entry.storageId, entry.id, refresh = true).forEach { walk(it, "$relative/${it.name}") }
            }
        }
        walk(root, root.name)
        val total = plan.sumOf { if (it.first.isDirectory) 0 else it.first.size }.coerceAtLeast(1)
        var done = 0L
        var files = 0
        for ((entry, relative) in plan) {
            ensureActive()
            val target = File(localDirectory, relative)
            if (entry.isDirectory) {
                target.mkdirs()
                continue
            }
            target.parentFile?.mkdirs()
            val callback = progressCallback(this, done, total, entry.name, onProgress)
            val rc = lib.LIBMTP_Get_File_To_File(device.handle, entry.id, target.absolutePath, callback, null)
            ensureActive()
            if (rc != 0) throw TransferFailedException(lastError(lib, device, "Could not copy ${entry.name}"))
            done += entry.size
            files++
        }
        "$files ${if (files == 1) "file" else "files"} copied"
    }

    /** Copies a local file or folder into the device folder [remoteDirectory], replacing same-name files. */
    suspend fun upload(
        serial: String,
        localPath: String,
        remoteDirectory: String,
        onProgress: (TransferProgress) -> Unit,
    ): String = onMtpThread { lib ->
        val device = device(serial)
        val segments = MtpPaths.segments(remoteDirectory)
        if (segments.isEmpty()) throw TransferFailedException("Open a storage (e.g. Internal shared storage) before copying")
        val (storageId, folderId) = resolveFolder(lib, device, segments)
        val source = File(localPath)
        if (!source.exists()) throw TransferFailedException("$localPath no longer exists")
        val total = source.walkTopDown().filter { it.isFile }.sumOf { it.length() }.coerceAtLeast(1)
        var done = 0L
        var files = 0

        suspend fun CoroutineScope.send(file: File, parentId: Int) {
            ensureActive()
            if (file.isDirectory) {
                val existing = children(lib, device, storageId, parentId).firstOrNull { it.name == file.name }
                val folderIdNew = when {
                    existing?.isDirectory == true -> existing.id
                    else -> lib.LIBMTP_Create_Folder(device.handle, file.name, parentId, storageId)
                        .also { device.listings.remove(storageId to parentId) }
                }
                if (folderIdNew == 0) throw TransferFailedException(lastError(lib, device, "Could not create ${file.name}"))
                file.listFiles()?.sortedBy { it.name }?.forEach { send(it, folderIdNew) }
                return
            }
            // Match adb push: a file with the same name is replaced.
            children(lib, device, storageId, parentId).firstOrNull { it.name == file.name && !it.isDirectory }?.let {
                lib.LIBMTP_Delete_Object(device.handle, it.id)
            }
            val name = Memory(file.name.toByteArray().size + 1L).apply { setString(0, file.name) }
            val meta = MtpFileStruct().apply {
                parent_id = parentId
                storage_id = storageId
                filename = name
                filesize = file.length()
                filetype = unknownType
                modificationdate = file.lastModified() / 1000
            }
            val callback = progressCallback(this, done, total, file.name, onProgress)
            val rc = lib.LIBMTP_Send_File_From_File(device.handle, file.absolutePath, meta, callback, null)
            device.listings.remove(storageId to parentId)
            ensureActive()
            if (rc != 0) throw TransferFailedException(lastError(lib, device, "Could not copy ${file.name}"))
            done += file.length()
            files++
        }

        send(source, folderId)
        "$files ${if (files == 1) "file" else "files"} copied"
    }

    private fun progressCallback(
        scope: CoroutineScope,
        done: Long,
        total: Long,
        item: String,
        onProgress: (TransferProgress) -> Unit,
    ) = MtpProgressCallback { sent, _, _ ->
        onProgress(TransferProgress((((done + sent) * 100) / total).toInt().coerceIn(0, 100), item))
        if (scope.isActive) 0 else 1 // non-zero asks libmtp to cancel
    }

    // ---------------------------------------------------------------- path resolution

    private fun device(serial: String): OpenDevice =
        open[serial] ?: throw TransferFailedException("The device is not available. Reconnect it and try again.")

    /** Returns (storageId, folderId) for a folder path; a storage's top level has folderId [MTP_ROOT]. */
    private fun resolveFolder(lib: LibMtpNative, device: OpenDevice, segments: List<String>): Pair<Int, Int> {
        require(segments.isNotEmpty()) { "Choose a storage first" }
        val storage = device.storages.firstOrNull { it.name == segments.first() }
            ?: throw TransferFailedException("Storage \"${segments.first()}\" not found")
        var folderId = MTP_ROOT
        for (name in segments.drop(1)) {
            val child = children(lib, device, storage.id, folderId).firstOrNull { it.name == name }
                ?: children(lib, device, storage.id, folderId, refresh = true).firstOrNull { it.name == name }
                ?: throw TransferFailedException("Folder \"$name\" not found")
            if (!child.isDirectory) throw TransferFailedException("\"$name\" is not a folder")
            folderId = child.id
        }
        return storage.id to folderId
    }

    private fun lookup(lib: LibMtpNative, device: OpenDevice, segments: List<String>): Entry {
        if (segments.size < 2) throw TransferFailedException("Storages cannot be changed")
        val (storageId, parentId) = resolveFolder(lib, device, segments.dropLast(1))
        val name = segments.last()
        return children(lib, device, storageId, parentId).firstOrNull { it.name == name }
            ?: children(lib, device, storageId, parentId, refresh = true).firstOrNull { it.name == name }
            ?: throw TransferFailedException("\"$name\" not found")
    }

    private fun children(lib: LibMtpNative, device: OpenDevice, storageId: Int, parentId: Int, refresh: Boolean = false): List<Entry> {
        val key = storageId to parentId
        if (!refresh) device.listings[key]?.let { return it }
        val entries = mutableListOf<Entry>()
        var p = lib.LIBMTP_Get_Files_And_Folders(device.handle, storageId, parentId)
        while (p != null) {
            val f = MtpFileStruct(p)
            entries += Entry(
                id = f.item_id,
                storageId = f.storage_id,
                name = f.filename?.getString(0) ?: "",
                isDirectory = f.filetype == folderType,
                size = f.filesize,
                modified = f.modificationdate,
            )
            val next = f.next
            lib.LIBMTP_destroy_file_t(p)
            p = next
        }
        return entries.filter { it.name.isNotEmpty() }.also { device.listings[key] = it }
    }

    // ---------------------------------------------------------------- helpers

    /** Looks up libmtp's enum values by description instead of hard-coding them. */
    private fun resolveFiletypes(lib: LibMtpNative) {
        for (i in 0..200) {
            when (lib.LIBMTP_Get_Filetype_Description(i)?.getString(0)) {
                "Folder" -> folderType = i
                "Unknown filetype" -> {
                    unknownType = i
                    return
                }
            }
        }
        unknownType = 44 // LIBMTP_FILETYPE_UNKNOWN in libmtp 1.1.x
    }

    private fun lastError(lib: LibMtpNative, device: OpenDevice, fallback: String): String {
        val messages = buildList {
            var p = lib.LIBMTP_Get_Errorstack(device.handle)
            while (p != null) {
                val e = MtpErrorStruct(p)
                e.error_text?.getString(0)?.trim()?.takeIf { it.isNotEmpty() }?.let(::add)
                p = e.next
            }
        }
        lib.LIBMTP_Clear_Errorstack(device.handle)
        return messages.lastOrNull()?.let { "$fallback: $it" } ?: fallback
    }

    private fun Pointer?.takeString(): String? {
        if (this == null) return null
        return getString(0).also { Native.free(Pointer.nativeValue(this)) }.takeIf { it.isNotBlank() }
    }

    private companion object {
        const val BUSY_RETRY_POLLS = 2
    }
}
