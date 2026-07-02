package com.autoclock

import java.util.Calendar

object ChineseHolidayCalendar {

    private val holidayKeys = setOf(
        // 2025 国务院办公厅放假安排：春节、劳动节、端午节、国庆节/中秋节。
        dateKey(2025, 1, 28), dateKey(2025, 1, 29), dateKey(2025, 1, 30), dateKey(2025, 1, 31),
        dateKey(2025, 2, 1), dateKey(2025, 2, 2), dateKey(2025, 2, 3), dateKey(2025, 2, 4),
        dateKey(2025, 5, 1), dateKey(2025, 5, 2), dateKey(2025, 5, 3), dateKey(2025, 5, 4),
        dateKey(2025, 5, 5),
        dateKey(2025, 5, 31), dateKey(2025, 6, 1), dateKey(2025, 6, 2),
        dateKey(2025, 10, 1), dateKey(2025, 10, 2), dateKey(2025, 10, 3), dateKey(2025, 10, 4),
        dateKey(2025, 10, 5), dateKey(2025, 10, 6), dateKey(2025, 10, 7), dateKey(2025, 10, 8),

        // 2026 国务院办公厅放假安排：春节、劳动节、端午节、中秋节、国庆节。
        dateKey(2026, 2, 15), dateKey(2026, 2, 16), dateKey(2026, 2, 17), dateKey(2026, 2, 18),
        dateKey(2026, 2, 19), dateKey(2026, 2, 20), dateKey(2026, 2, 21), dateKey(2026, 2, 22),
        dateKey(2026, 2, 23),
        dateKey(2026, 5, 1), dateKey(2026, 5, 2), dateKey(2026, 5, 3), dateKey(2026, 5, 4),
        dateKey(2026, 5, 5),
        dateKey(2026, 6, 19), dateKey(2026, 6, 20), dateKey(2026, 6, 21),
        dateKey(2026, 9, 25), dateKey(2026, 9, 26), dateKey(2026, 9, 27),
        dateKey(2026, 10, 1), dateKey(2026, 10, 2), dateKey(2026, 10, 3), dateKey(2026, 10, 4),
        dateKey(2026, 10, 5), dateKey(2026, 10, 6), dateKey(2026, 10, 7)
    )

    fun isBusinessDay(calendar: Calendar): Boolean {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) return false
        return !isChinesePublicHoliday(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1,
            dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun isChinesePublicHoliday(year: Int, month: Int, dayOfMonth: Int): Boolean {
        return holidayKeys.contains(dateKey(year, month, dayOfMonth))
    }

    private fun dateKey(year: Int, month: Int, dayOfMonth: Int): Int {
        return year * 10_000 + month * 100 + dayOfMonth
    }
}
