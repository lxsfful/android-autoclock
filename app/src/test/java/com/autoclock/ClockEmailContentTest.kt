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

    @Test
    fun `result subject and body are success notification when task succeeds`() {
        assertEquals("自动操作成功，任务时间08:05", ClockEmailContent.resultSubject(success = true, "08:05"))
        assertEquals("自动操作成功，任务时间08:05", ClockEmailContent.resultBody(success = true, "", "08:05"))
    }

    @Test
    fun `result subject and body are manual reminder when task is not confirmed`() {
        assertEquals("自动化任务未确认，请手动检查", ClockEmailContent.resultSubject(success = false, "08:05"))
        assertEquals(
            "自动化任务未确认，请手动检查\n时间：08:05\n原因：已有任务序列正在执行，本次触发未执行，请手动检查",
            ClockEmailContent.resultBody(success = false, "已有任务序列正在执行，本次触发未执行，请手动检查", "08:05")
        )
    }

    @Test
    fun `failure body includes reminder time and reason`() {
        assertEquals(
            "自动化任务未确认，请手动检查\n时间：08:05\n原因：无障碍服务未运行，请在系统设置中开启",
            ClockEmailContent.failureBody("无障碍服务未运行，请在系统设置中开启", "08:05")
        )
    }

    @Test
    fun `failure body uses fallback for blank reason`() {
        assertEquals(
            "自动化任务未确认，请手动检查\n时间：08:05\n原因：未知原因",
            ClockEmailContent.failureBody("", "08:05")
        )
    }
}
