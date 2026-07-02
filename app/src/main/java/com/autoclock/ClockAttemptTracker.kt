package com.autoclock

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ClockAttemptTracker {

    const val MAX_ATTEMPTS_PER_WINDOW = 2

    fun localDateKey(timestampMs: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestampMs))
    }

    fun canAttempt(currentCount: Int): Boolean {
        return currentCount < MAX_ATTEMPTS_PER_WINDOW
    }

    internal fun countForDate(storedDate: String, storedCount: Int, dateKey: String): Int {
        return if (storedDate == dateKey) storedCount else 0
    }

    internal fun incrementedCountForDate(storedDate: String, storedCount: Int, dateKey: String): Int {
        return countForDate(storedDate, storedCount, dateKey) + 1
    }

    fun getAttemptCount(prefs: Prefs, isClockIn: Boolean, dateKey: String = localDateKey()): Int {
        resetIfDateChanged(prefs, isClockIn, dateKey)
        return if (isClockIn) prefs.clockInAttemptCount else prefs.clockOutAttemptCount
    }

    fun canAttempt(prefs: Prefs, isClockIn: Boolean, dateKey: String = localDateKey()): Boolean {
        return canAttempt(getAttemptCount(prefs, isClockIn, dateKey))
    }

    fun incrementAttemptCount(prefs: Prefs, isClockIn: Boolean, dateKey: String = localDateKey()): Int {
        val storedDate = if (isClockIn) prefs.clockInAttemptDate else prefs.clockOutAttemptDate
        val storedCount = if (isClockIn) prefs.clockInAttemptCount else prefs.clockOutAttemptCount
        val nextCount = incrementedCountForDate(storedDate, storedCount, dateKey)
        if (isClockIn) {
            prefs.clockInAttemptDate = dateKey
            prefs.clockInAttemptCount = nextCount
        } else {
            prefs.clockOutAttemptDate = dateKey
            prefs.clockOutAttemptCount = nextCount
        }
        return nextCount
    }

    fun resetIfDateChanged(prefs: Prefs, isClockIn: Boolean, dateKey: String = localDateKey()) {
        if (isClockIn) {
            if (prefs.clockInAttemptDate != dateKey) {
                prefs.clockInAttemptDate = dateKey
                prefs.clockInAttemptCount = 0
            }
        } else if (prefs.clockOutAttemptDate != dateKey) {
            prefs.clockOutAttemptDate = dateKey
            prefs.clockOutAttemptCount = 0
        }
    }
}
