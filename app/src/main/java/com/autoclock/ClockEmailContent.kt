package com.autoclock

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ClockEmailContent {
    const val FAILURE_MESSAGE = "自动化任务未确认，请手动检查"
    const val SUCCESS_MESSAGE = "自动操作成功，任务时间"

    fun successSubject(timeText: String): String {
        return "$SUCCESS_MESSAGE$timeText"
    }

    fun formatClockTime(timeMillis: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMillis))
    }
}
