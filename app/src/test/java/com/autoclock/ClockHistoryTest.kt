package com.autoclock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockHistoryTest {

    @Test
    fun `load returns empty list for blank input`() {
        assertTrue(ClockHistory.load("").isEmpty())
    }

    @Test
    fun `load returns empty list for malformed json`() {
        assertTrue(ClockHistory.load("not json").isEmpty())
    }

    @Test
    fun `append to empty json stores one record`() {
        val json = ClockHistory.append("", record(timestampMs = NOW_MS, success = true), NOW_MS)

        val records = ClockHistory.load(json)
        assertEquals(1, records.size)
        assertEquals(NOW_MS, records.first().timestampMs)
        assertEquals(true, records.first().success)
    }

    @Test
    fun `append stores newest records first`() {
        val older = record(timestampMs = NOW_MS - 1_000L, isClockIn = true)
        val newer = record(timestampMs = NOW_MS, isClockIn = false)

        val json = ClockHistory.append(ClockHistory.append("", older, NOW_MS), newer, NOW_MS)

        val records = ClockHistory.load(json)
        assertEquals(listOf(newer, older), records)
    }

    @Test
    fun `append drops records older than 30 days`() {
        val old = record(timestampMs = NOW_MS - THIRTY_ONE_DAYS_MS, reason = "旧记录")
        val fresh = record(timestampMs = NOW_MS, reason = "新记录")

        val jsonWithOldRecord = ClockHistory.serialize(listOf(old))
        val updated = ClockHistory.append(jsonWithOldRecord, fresh, NOW_MS)

        assertEquals(listOf(fresh), ClockHistory.load(updated))
    }

    @Test
    fun `append keeps records exactly 30 days old`() {
        val boundary = record(timestampMs = NOW_MS - THIRTY_DAYS_MS, reason = "边界记录")
        val fresh = record(timestampMs = NOW_MS, reason = "新记录")

        val updated = ClockHistory.append(ClockHistory.serialize(listOf(boundary)), fresh, NOW_MS)

        assertEquals(listOf(fresh, boundary), ClockHistory.load(updated))
    }

    @Test
    fun `round trip preserves failure reason with Chinese characters`() {
        val failed = record(success = false, reason = "等待目标 App操作成功提示超时")

        val records = ClockHistory.load(ClockHistory.serialize(listOf(failed)))

        assertEquals(failed, records.single())
    }

    private fun record(
        timestampMs: Long = NOW_MS,
        isClockIn: Boolean = true,
        success: Boolean = false,
        reason: String = "测试原因"
    ): ClockRecord {
        return ClockRecord(
            timestampMs = timestampMs,
            isClockIn = isClockIn,
            success = success,
            reason = reason
        )
    }

    private companion object {
        const val NOW_MS = 1_700_000_000_000L
        const val THIRTY_DAYS_MS = 30L * 24L * 60L * 60L * 1000L
        const val THIRTY_ONE_DAYS_MS = 31L * 24L * 60L * 60L * 1000L
    }
}
