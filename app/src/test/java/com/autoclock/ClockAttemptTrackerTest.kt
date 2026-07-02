package com.autoclock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockAttemptTrackerTest {

    @Test
    fun `allows first and second attempts only`() {
        assertTrue(ClockAttemptTracker.canAttempt(0))
        assertTrue(ClockAttemptTracker.canAttempt(1))
        assertFalse(ClockAttemptTracker.canAttempt(2))
        assertFalse(ClockAttemptTracker.canAttempt(3))
    }

    @Test
    fun `same date keeps stored count`() {
        assertEquals(1, ClockAttemptTracker.countForDate("2026-06-30", 1, "2026-06-30"))
    }

    @Test
    fun `new date resets stored count`() {
        assertEquals(0, ClockAttemptTracker.countForDate("2026-06-29", 2, "2026-06-30"))
    }

    @Test
    fun `incrementing same date adds one`() {
        assertEquals(2, ClockAttemptTracker.incrementedCountForDate("2026-06-30", 1, "2026-06-30"))
    }

    @Test
    fun `incrementing new date starts at one`() {
        assertEquals(1, ClockAttemptTracker.incrementedCountForDate("2026-06-29", 2, "2026-06-30"))
    }
}
