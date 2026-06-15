package com.autoclock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundAppCheckerTest {

    @Test
    fun `returns ready when target package is present on first attempt`() {
        val decision = ForegroundAppChecker.evaluate(listOf(TARGET_PACKAGE), TARGET_PACKAGE)

        assertTrue(decision.isTargetReady)
        assertNull(decision.failureReason)
    }

    @Test
    fun `returns ready when target package appears during retry`() {
        val decision = ForegroundAppChecker.evaluate(
            listOf("com.android.launcher", "com.autoclock", TARGET_PACKAGE),
            TARGET_PACKAGE
        )

        assertTrue(decision.isTargetReady)
        assertNull(decision.failureReason)
    }

    @Test
    fun `returns unreadable failure when active window package is never readable`() {
        val decision = ForegroundAppChecker.evaluate(listOf(null, null, null), TARGET_PACKAGE)

        assertFalse(decision.isTargetReady)
        assertEquals(ForegroundAppChecker.UNREADABLE_REASON, decision.failureReason)
    }

    @Test
    fun `returns not foreground failure when another package remains foreground`() {
        val decision = ForegroundAppChecker.evaluate(
            listOf("com.android.launcher", "com.android.launcher"),
            TARGET_PACKAGE
        )

        assertFalse(decision.isTargetReady)
        assertEquals(ForegroundAppChecker.NOT_FOREGROUND_REASON, decision.failureReason)
    }

    private companion object {
        const val TARGET_PACKAGE = "com.kdweibo.client"
    }
}
