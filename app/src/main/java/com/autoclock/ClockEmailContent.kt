package com.autoclock

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ClockEmailContent {
    const val FAILURE_MESSAGE = "打卡不成功，请手动打卡"
    const val SUCCESS_MESSAGE = "打卡成功，请手动查看核实"

    fun successSubject(timeText: String): String {
        return SUCCESS_MESSAGE
    }

    fun formatClockTime(timeMillis: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMillis))
    }
}
