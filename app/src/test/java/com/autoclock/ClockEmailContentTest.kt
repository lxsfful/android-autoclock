package com.autoclock

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockEmailContentTest {

    @Test
    fun `failure message matches manual clock reminder exactly`() {
        assertEquals("自动化任务未确认，请手动检查", ClockEmailContent.FAILURE_MESSAGE)
    }

    @Test
    fun `success subject includes clock time`() {
        assertEquals("自动操作成功，任务时间08:05", ClockEmailContent.successSubject("08:05"))
    }
}
