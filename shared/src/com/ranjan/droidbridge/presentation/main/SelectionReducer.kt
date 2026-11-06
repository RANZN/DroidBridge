package com.ranjan.droidbridge.presentation.main

/**
 * Pure selection rules (Finder-style), kept out of the ViewModel so they are easy to test.
 *
 * - [MainState.focusedPath] is the keyboard cursor (last clicked or moved-to item).
 * - [MainState.anchorPath] is where a Shift range starts.
 */
object SelectionReducer {

    fun click(state: MainState, path: String, mode: SelectionMode): MainState = when (mode) {
        SelectionMode.Replace -> state.copy(selectedPaths = setOf(path), focusedPath = path, anchorPath = path)
        SelectionMode.Toggle -> state.copy(
            selectedPaths = if (path in state.selectedPaths) state.selectedPaths - path else state.selectedPaths + path,
            focusedPath = path,
            anchorPath = path,
        )
        SelectionMode.Range -> {
            val anchor = state.anchorPath?.takeIf { a -> state.files.any { it.path == a } } ?: path
            state.copy(selectedPaths = range(state, anchor, path), focusedPath = path, anchorPath = anchor)
        }
    }

    /** Moves the cursor by [delta] items (±1 in a list, ±columns for up/down in a grid). */
    fun move(state: MainState, delta: Int, extend: Boolean): MainState {
        if (state.files.isEmpty()) return state
        val current = state.files.indexOfFirst { it.path == state.focusedPath }
        val target = when {
            current == -1 -> if (delta > 0) 0 else state.files.lastIndex
            else -> (current + delta).coerceIn(0, state.files.lastIndex)
        }
        return moveTo(state, target, extend)
    }

    /** Jumps to the first or last item (Home / End). */
    fun edge(state: MainState, toEnd: Boolean, extend: Boolean): MainState {
        if (state.files.isEmpty()) return state
        return moveTo(state, if (toEnd) state.files.lastIndex else 0, extend)
    }

    fun selectAll(state: MainState): MainState =
        state.copy(selectedPaths = state.files.map { it.path }.toSet())

    fun clear(state: MainState): MainState =
        state.copy(selectedPaths = emptySet(), anchorPath = null)

    /** Drops selection/cursor entries that no longer exist after a reload. */
    fun retainExisting(state: MainState): MainState {
        val existing = state.files.map { it.path }.toSet()
        return state.copy(
            selectedPaths = state.selectedPaths intersect existing,
            focusedPath = state.focusedPath?.takeIf { it in existing },
            anchorPath = state.anchorPath?.takeIf { it in existing },
        )
    }

    private fun moveTo(state: MainState, index: Int, extend: Boolean): MainState {
        val path = state.files[index].path
        return if (extend) {
            val anchor = state.anchorPath ?: state.focusedPath ?: path
            state.copy(selectedPaths = range(state, anchor, path), focusedPath = path, anchorPath = anchor)
        } else {
            state.copy(selectedPaths = setOf(path), focusedPath = path, anchorPath = path)
        }
    }

    private fun range(state: MainState, from: String, to: String): Set<String> {
        val a = state.files.indexOfFirst { it.path == from }
        val b = state.files.indexOfFirst { it.path == to }
        if (a == -1 || b == -1) return setOf(to)
        return state.files.subList(minOf(a, b), maxOf(a, b) + 1).map { it.path }.toSet()
    }
}
