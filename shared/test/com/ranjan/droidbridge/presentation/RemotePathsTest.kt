package com.ranjan.droidbridge.presentation

import com.ranjan.droidbridge.domain.usecase.RemotePaths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RemotePathsTest {
    @Test
    fun `parent and breadcrumbs`() {
        assertEquals("/sdcard", RemotePaths.parent("/sdcard/DCIM"))
        assertEquals("/", RemotePaths.parent("/sdcard"))
        assertNull(RemotePaths.parent("/"))
        assertEquals(listOf("/", "/sdcard", "/sdcard/DCIM"), RemotePaths.breadcrumbs("/sdcard/DCIM/"))
        assertEquals("/sdcard/a", RemotePaths.child("/sdcard/", "a"))
    }
}
