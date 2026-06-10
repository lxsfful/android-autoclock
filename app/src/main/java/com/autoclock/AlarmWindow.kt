package com.autoclock

object AlarmWindow {
    fun windowMinutes(hStart: Int, mStart: Int, hEnd: Int, mEnd: Int): Int {
        return (hEnd - hStart) * 60 + (mEnd - mStart)
    }

    fun isValid(hStart: Int, mStart: Int, hEnd: Int, mEnd: Int): Boolean {
        return windowMinutes(hStart, mStart, hEnd, mEnd) > 0
    }
}
