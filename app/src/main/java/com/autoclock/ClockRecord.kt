package com.autoclock

data class ClockRecord(
    val timestampMs: Long,
    val isClockIn: Boolean,
    val success: Boolean,
    val reason: String
)
