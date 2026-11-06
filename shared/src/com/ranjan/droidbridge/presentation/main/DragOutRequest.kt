package com.ranjan.droidbridge.presentation.main

import com.ranjan.droidbridge.domain.model.StagedFile
import kotlinx.coroutines.Deferred

/**
 * Handle for one drag of device files out of the app.
 *
 * Nothing is pulled from the device until a drop target actually asks for the files
 * ([localPaths]). Finder asks when the files are dropped on it; a drop back onto DroidBridge,
 * or a cancelled drag, never asks, so no transfer happens.
 */
class DragOutRequest internal constructor(start: () -> Deferred<List<StagedFile>>) {

    private val staging = lazy(LazyThreadSafetyMode.SYNCHRONIZED, start)

    /** Whether a drop target has asked for the files yet. */
    val isStarted: Boolean get() = staging.isInitialized()

    /** Pulls the files (once) and returns their local paths. Blocks the drop until done. */
    suspend fun localPaths(): List<String> = staging.value.await().map { it.localPath }

    internal val stagingOrNull: Deferred<List<StagedFile>>? get() = if (isStarted) staging.value else null
}
