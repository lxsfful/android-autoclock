package com.autoclock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ChineseHolidayCalendarTest {

    @Test
    fun `regular weekday is business day`() {
        assertTrue(ChineseHolidayCalendar.isBusinessDay(day(2026, 2, 13)))
    }

    @Test
    fun `weekend is not business day`() {
        assertFalse(ChineseHolidayCalendar.isBusinessDay(day(2026, 6, 20)))
        assertFalse(ChineseHolidayCalendar.isBusinessDay(day(2026, 6, 21)))
    }

    @Test
    fun `configured Chinese public holidays are not business days`() {
        assertFalse(ChineseHolidayCalendar.isBusinessDay(day(2026, 2, 17)))
        assertFalse(ChineseHolidayCalendar.isBusinessDay(day(2026, 5, 1)))
        assertFalse(ChineseHolidayCalendar.isBusinessDay(day(2026, 6, 19)))
        assertFalse(ChineseHolidayCalendar.isBusinessDay(day(2026, 9, 25)))
        assertFalse(ChineseHolidayCalendar.isBusinessDay(day(2026, 10, 1)))
    }

    @Test
    fun `holiday range boundary returns to business day after Spring Festival`() {
        assertFalse(ChineseHolidayCalendar.isBusinessDay(day(2026, 2, 23)))
        assertTrue(ChineseHolidayCalendar.isBusinessDay(day(2026, 2, 24)))
    }

    @Test
    fun `holiday lookup uses one based month`() {
        assertTrue(ChineseHolidayCalendar.isChinesePublicHoliday(2026, 5, 1))
        assertFalse(ChineseHolidayCalendar.isChinesePublicHoliday(2026, 4, 1))
    }

    private fun day(year: Int, month: Int, dayOfMonth: Int): Calendar {
        return Calendar.getInstance().apply {
            set(year, month - 1, dayOfMonth, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
