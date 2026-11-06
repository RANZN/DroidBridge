package com.ranjan.adbsend.data.adb

import com.ranjan.adbsend.domain.model.DeviceState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdbParsersTest {

    @Test
    fun `parses multiple devices with states and models`() {
        val output = """
            * daemon started successfully
            List of devices attached
            emulator-5554          device product:sdk_gphone64 model:sdk_gphone64_arm64 device:emu64a transport_id:2
            R58M123ABC             unauthorized usb:1-1 transport_id:3
            192.168.1.20:5555      offline transport_id:4

        """.trimIndent()

        val devices = AdbParsers.parseDevices(output)

        assertEquals(3, devices.size)
        assertEquals("emulator-5554", devices[0].serial)
        assertEquals(DeviceState.Online, devices[0].state)
        assertEquals("sdk gphone64 arm64", devices[0].displayName)
        assertEquals(DeviceState.Unauthorized, devices[1].state)
        assertEquals("R58M123ABC", devices[1].displayName)
        assertEquals(DeviceState.Offline, devices[2].state)
    }

    @Test
    fun `parses stat listing including names with separators`() {
        val output = """
            directory|4096|1793776112|/sdcard/Android
            regular file|2048|1793776000|/sdcard/Download/report|final.pdf
            regular empty file|0|1793776000|/sdcard/empty.txt
            garbage line
        """.trimIndent()

        val files = AdbParsers.parseListing(output)

        assertEquals(3, files.size)
        assertTrue(files[0].isDirectory)
        assertEquals("Android", files[0].name)
        assertEquals("report|final.pdf", files[1].name)
        assertEquals(2048, files[1].sizeBytes)
        assertEquals(0, files[2].sizeBytes)
    }

    @Test
    fun `quotes single quotes for the device shell`() {
        assertEquals("'it'\\''s here'", shellQuote("it's here"))
    }

    @Test
    fun `parses progress lines for files and folders`() {
        assertEquals(AdbProgress(86, "/sdcard/Download/big.bin"), AdbParsers.parseProgress("[ 86%] /sdcard/Download/big.bin"))
        assertEquals(AdbProgress(35, "/sdcard/Download/d/b.bin"), AdbParsers.parseProgress("[ 35%] /sdcard/Download/d/b.bin: 71%"))
        assertEquals(AdbProgress(100, "/sdcard/x"), AdbParsers.parseProgress("[100%] /sdcard/x"))
        assertEquals(null, AdbParsers.parseProgress("big.bin: 1 file pushed, 0 skipped."))
    }

    @Test
    fun `cleans terminal escape codes and control characters`() {
        assertEquals("[ 86%] /sdcard/a.bin", cleanTerminalOutput("\u0004\b\b[ 86%] /sdcard/a.bin\u001B[K"))
        assertEquals("adb: error: cannot stat 'x'", cleanTerminalOutput("\u0004\b\badb: error: cannot stat 'x'"))
    }

    @Test
    fun `error message prefers the adb error line over a summary`() {
        val result = AdbResult(1, "a.txt: 1 file pushed, 0 skipped.\nadb: error: remote couldn't create file", "")
        assertEquals("adb: error: remote couldn't create file", result.errorMessage)
    }
}
