package com.ranjan.droidbridge.data.repository

import com.ranjan.droidbridge.domain.model.FileViewMode
import java.util.UUID
import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PreferencesSettingsRepositoryTest {
    // A throwaway node, so tests never touch the real app settings.
    private val node = Preferences.userRoot().node("com/ranjan/droidbridge-test/${UUID.randomUUID()}")

    @AfterTest
    fun cleanUp() = node.removeNode()

    @Test
    fun `values persist in the injected preferences node`() {
        val repo = PreferencesSettingsRepository(node)
        assertFalse(repo.mtpEnabled.value)
        repo.setMtpEnabled(true)
        repo.fileViewMode = FileViewMode.Grid
        repo.isSetupGuideDismissed = true

        val reloaded = PreferencesSettingsRepository(node)
        assertTrue(reloaded.mtpEnabled.value)
        assertEquals(FileViewMode.Grid, reloaded.fileViewMode)
        assertTrue(reloaded.isSetupGuideDismissed)
    }
}
