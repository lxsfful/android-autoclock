package com.autoclock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmWindowTest {

    @Test
    fun `window minutes supports clock out window after noon`() {
        assertEquals(29, AlarmWindow.windowMinutes(18, 1, 18, 30))
    }

    @Test
    fun `validates clock out window after noon`() {
        assertTrue(AlarmWindow.isValid(18, 1, 18, 30))
    }

    @Test
    fun `rejects zero length window`() {
        assertFalse(AlarmWindow.isValid(18, 0, 18, 0))
    }

    @Test
    fun `rejects backwards window`() {
        assertFalse(AlarmWindow.isValid(18, 30, 18, 1))
    }
}
