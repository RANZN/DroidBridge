package com.ranjan.droidbridge.data.mtp

import com.sun.jna.Structure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MtpBindingTest {

    private fun Structure.fieldOffset(name: String): Int =
        Structure::class.java.getDeclaredMethod("fieldOffset", String::class.java)
            .apply { isAccessible = true }
            .invoke(this, name) as Int

    /** Sizes/offsets of libmtp's C structs on 64-bit macOS; a mismatch would corrupt memory. */
    @Test
    fun `jna struct layouts match libmtp on 64-bit`() {
        assertEquals(40, MtpRawDevice().size())
        assertEquals(32, MtpRawDevice().fieldOffset("bus_location"))
        assertEquals(56, MtpFileStruct().size())
        assertEquals(16, MtpFileStruct().fieldOffset("filename"))
        assertEquals(40, MtpFileStruct().fieldOffset("filetype"))
        assertEquals(48, MtpFileStruct().fieldOffset("next"))
        assertEquals(72, MtpStorageStruct().size())
        assertEquals(40, MtpStorageStruct().fieldOffset("StorageDescription"))
        assertEquals(24, MtpDeviceHead().fieldOffset("storage"))
        assertEquals(8, MtpErrorStruct().fieldOffset("error_text"))
    }

    @Test
    fun `device ids and paths`() {
        assertTrue(MtpDeviceIds.isMtp(MtpDeviceIds.of(20, 5)))
        assertFalse(MtpDeviceIds.isMtp("emulator-5554"))
        assertEquals(listOf("Internal shared storage", "DCIM"), MtpPaths.segments("/Internal shared storage/DCIM/"))
        assertEquals("/a/b", MtpPaths.join(listOf("a", "b")))
        assertEquals(listOf("SD card", "SD card (2)", "Storage"), MtpPaths.uniqueStorageNames(listOf("SD card", "SD card", " ")))
    }
}
