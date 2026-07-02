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

    fun resultSubject(success: Boolean, timeText: String): String {
        return if (success) successSubject(timeText) else FAILURE_MESSAGE
    }

    fun resultBody(success: Boolean, reason: String, timeText: String): String {
        return if (success) successSubject(timeText) else failureBody(reason, timeText)
    }

    fun failureBody(reason: String, timeText: String): String {
        val safeReason = reason.ifBlank { "未知原因" }
        return "$FAILURE_MESSAGE\n时间：$timeText\n原因：$safeReason"
    }

    fun formatClockTime(timeMillis: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMillis))
    }
}
