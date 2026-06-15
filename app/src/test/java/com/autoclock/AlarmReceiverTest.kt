package com.autoclock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AlarmReceiverTest {

    @Test
    fun `builds service not running history record for clock in alarm`() {
        val record = AlarmReceiver.missingServiceRecord(isClockIn = true, timestampMs = TIMESTAMP_MS)

        assertEquals(TIMESTAMP_MS, record.timestampMs)
        assertEquals(true, record.isClockIn)
        assertFalse(record.success)
        assertEquals(AlarmReceiver.SERVICE_NOT_RUNNING_REASON, record.reason)
    }

    @Test
    fun `builds service not running history record for clock out alarm`() {
        val record = AlarmReceiver.missingServiceRecord(isClockIn = false, timestampMs = TIMESTAMP_MS)

        assertEquals(TIMESTAMP_MS, record.timestampMs)
        assertEquals(false, record.isClockIn)
        assertFalse(record.success)
        assertEquals(AlarmReceiver.SERVICE_NOT_RUNNING_REASON, record.reason)
    }

    @Test
    fun `appends service not running record without dropping fresh history`() {
        val existingRecord = ClockRecord(
            timestampMs = TIMESTAMP_MS - 1_000L,
            isClockIn = true,
            success = true,
            reason = ""
        )
        val missingServiceRecord = AlarmReceiver.missingServiceRecord(
            isClockIn = false,
            timestampMs = TIMESTAMP_MS
        )

        val existingJson = ClockHistory.append("", existingRecord)
        val updatedHistory = ClockHistory.load(ClockHistory.append(existingJson, missingServiceRecord))

        assertEquals(listOf(missingServiceRecord, existingRecord), updatedHistory)
    }

    private companion object {
        const val TIMESTAMP_MS = 4_000_000_000_000L
    }
}
