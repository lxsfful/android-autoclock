package com.autoclock

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AlarmSchedulerTest {

    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `normal schedule before window uses today`() {
        val triggerAt = compute(
            nowMs = time(2026, 2, 13, 7, 0),
            mode = AlarmScheduler.ScheduleMode.NORMAL,
            offset = 60_000L
        )

        assertEquals(time(2026, 2, 13, 8, 31), triggerAt)
    }

    @Test
    fun `normal schedule inside window moves to next business day`() {
        val triggerAt = compute(
            nowMs = time(2026, 2, 13, 8, 32),
            mode = AlarmScheduler.ScheduleMode.NORMAL,
            offset = 0L
        )

        assertEquals(time(2026, 2, 24, 8, 30), triggerAt)
    }

    @Test
    fun `normal schedule after window skips weekend and holiday range`() {
        val triggerAt = compute(
            nowMs = time(2026, 2, 13, 9, 0),
            mode = AlarmScheduler.ScheduleMode.NORMAL,
            offset = 0L
        )

        assertEquals(time(2026, 2, 24, 8, 30), triggerAt)
    }

    @Test
    fun `normal schedule on holiday moves to next business day`() {
        val triggerAt = compute(
            nowMs = time(2026, 5, 1, 7, 0),
            mode = AlarmScheduler.ScheduleMode.NORMAL,
            offset = 0L
        )

        assertEquals(time(2026, 5, 6, 8, 30), triggerAt)
    }

    @Test
    fun `retry inside current window uses current window when cap allows`() {
        val triggerAt = compute(
            nowMs = time(2026, 2, 13, 8, 32),
            mode = AlarmScheduler.ScheduleMode.RETRY_WITHIN_CURRENT_WINDOW,
            attemptCount = 1,
            offset = 30_000L
        )

        assertEquals(time(2026, 2, 13, 8, 35, 30), triggerAt)
    }

    @Test
    fun `retry at attempt cap falls back to next business day`() {
        val triggerAt = compute(
            nowMs = time(2026, 2, 13, 8, 32),
            mode = AlarmScheduler.ScheduleMode.RETRY_WITHIN_CURRENT_WINDOW,
            attemptCount = 2,
            offset = 0L
        )

        assertEquals(time(2026, 2, 24, 8, 30), triggerAt)
    }

    @Test
    fun `retry near window end falls back to next business day`() {
        val triggerAt = compute(
            nowMs = time(2026, 2, 13, 8, 38),
            mode = AlarmScheduler.ScheduleMode.RETRY_WITHIN_CURRENT_WINDOW,
            attemptCount = 1,
            offset = 0L
        )

        assertEquals(time(2026, 2, 24, 8, 30), triggerAt)
    }

    @Test
    fun `invalid window returns null`() {
        val triggerAt = AlarmScheduler.computeNextTrigger(
            nowMs = time(2026, 2, 13, 7, 0),
            window = AlarmScheduler.ClockWindow(8, 30, 8, 30),
            mode = AlarmScheduler.ScheduleMode.NORMAL,
            randomOffsetProvider = { 0L }
        )

        assertNull(triggerAt)
    }

    @Test
    fun `random provider receives window duration`() {
        var upperBound = 0L
        val triggerAt = AlarmScheduler.computeNextTrigger(
            nowMs = time(2026, 2, 13, 7, 0),
            window = AlarmScheduler.ClockWindow(8, 30, 8, 40),
            mode = AlarmScheduler.ScheduleMode.NORMAL,
            randomOffsetProvider = {
                upperBound = it
                0L
            }
        )

        assertNotNull(triggerAt)
        assertEquals(10L * 60L * 1000L, upperBound)
    }

    private fun compute(
        nowMs: Long,
        mode: AlarmScheduler.ScheduleMode,
        attemptCount: Int = ClockAttemptTracker.MAX_ATTEMPTS_PER_WINDOW,
        offset: Long
    ): Long? {
        return AlarmScheduler.computeNextTrigger(
            nowMs = nowMs,
            window = AlarmScheduler.ClockWindow(8, 30, 8, 40),
            mode = mode,
            currentAttemptCount = attemptCount,
            randomOffsetProvider = { upperBound ->
                assertTrue(offset < upperBound)
                offset
            }
        )
    }

    private fun time(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0): Long {
        return Calendar.getInstance().apply {
            set(year, month - 1, day, hour, minute, second)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
