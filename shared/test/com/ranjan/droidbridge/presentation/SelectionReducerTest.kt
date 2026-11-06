package com.ranjan.droidbridge.presentation

import com.ranjan.droidbridge.domain.model.RemoteFile
import com.ranjan.droidbridge.presentation.main.MainState
import com.ranjan.droidbridge.presentation.main.SelectionMode
import com.ranjan.droidbridge.presentation.main.SelectionReducer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SelectionReducerTest {
    private val files = (0..5).map { RemoteFile("/sdcard/f$it", "f$it", false, 0, 0) }
    private val base = MainState(files = files)
    private fun paths(vararg i: Int) = i.map { "/sdcard/f$it" }.toSet()

    @Test
    fun `arrow without shift moves a single selection`() {
        var s = SelectionReducer.move(base, 1, extend = false)
        assertEquals(paths(0), s.selectedPaths) // nothing focused yet -> first item
        s = SelectionReducer.move(s, 1, extend = false)
        assertEquals(paths(1), s.selectedPaths)
        assertEquals("/sdcard/f1", s.focusedPath)
    }

    @Test
    fun `shift arrow extends from the anchor and can shrink back`() {
        var s = SelectionReducer.click(base, "/sdcard/f2", SelectionMode.Replace)
        s = SelectionReducer.move(s, 1, extend = true)
        s = SelectionReducer.move(s, 1, extend = true)
        assertEquals(paths(2, 3, 4), s.selectedPaths)
        s = SelectionReducer.move(s, -3, extend = true)
        assertEquals(paths(1, 2), s.selectedPaths)
        assertEquals("/sdcard/f2", s.anchorPath)
    }

    @Test
    fun `movement is clamped at the ends`() {
        val s = SelectionReducer.move(SelectionReducer.click(base, "/sdcard/f5", SelectionMode.Replace), 3, false)
        assertEquals(paths(5), s.selectedPaths)
    }

    @Test
    fun `shift click selects a range and cmd click toggles`() {
        var s = SelectionReducer.click(base, "/sdcard/f1", SelectionMode.Replace)
        s = SelectionReducer.click(s, "/sdcard/f3", SelectionMode.Range)
        assertEquals(paths(1, 2, 3), s.selectedPaths)
        s = SelectionReducer.click(s, "/sdcard/f2", SelectionMode.Toggle)
        assertEquals(paths(1, 3), s.selectedPaths)
    }

    @Test
    fun `shift end selects to the last item`() {
        val s = SelectionReducer.edge(SelectionReducer.click(base, "/sdcard/f3", SelectionMode.Replace), toEnd = true, extend = true)
        assertEquals(paths(3, 4, 5), s.selectedPaths)
    }

    @Test
    fun `clear removes selection and anchor`() {
        val s = SelectionReducer.clear(SelectionReducer.selectAll(base.copy(anchorPath = "/sdcard/f1")))
        assertEquals(emptySet(), s.selectedPaths)
        assertNull(s.anchorPath)
    }

    @Test
    fun `reload drops paths that no longer exist`() {
        val s = SelectionReducer.retainExisting(
            base.copy(selectedPaths = paths(1, 9), focusedPath = "/sdcard/f9", anchorPath = "/sdcard/f1")
        )
        assertEquals(paths(1), s.selectedPaths)
        assertNull(s.focusedPath)
        assertEquals("/sdcard/f1", s.anchorPath)
    }
}
