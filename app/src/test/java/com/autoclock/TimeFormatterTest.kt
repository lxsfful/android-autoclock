package com.autoclock

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatterTest {

    @Test
    fun `formats 24 hour afternoon time`() {
        assertEquals("18:01", formatTimeLabel(18, 1))
    }

    @Test
    fun `formats midnight`() {
        assertEquals("00:00", formatTimeLabel(0, 0))
    }

    @Test
    fun `formats last minute of day`() {
        assertEquals("23:59", formatTimeLabel(23, 59))
    }
}
