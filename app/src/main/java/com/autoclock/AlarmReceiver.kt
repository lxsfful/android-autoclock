package com.autoclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val SERVICE_NOT_RUNNING_REASON = "无障碍服务未运行，请在系统设置中开启"

        internal fun missingServiceRecord(isClockIn: Boolean, timestampMs: Long): ClockRecord {
            return ClockRecord(
                timestampMs = timestampMs,
                isClockIn = isClockIn,
                success = false,
                reason = SERVICE_NOT_RUNNING_REASON
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val isClockIn = when (intent.action) {
            AlarmScheduler.ACTION_CLOCK_IN -> true
            AlarmScheduler.ACTION_CLOCK_OUT -> false
            else -> {
                Log.w("AlarmReceiver", "忽略未知闹钟 action: ${intent.action}")
                return
            }
        }
        Log.d("AlarmReceiver", "Alarm fired: clockIn=$isClockIn")

        val prefs = Prefs(context)
        if (!prefs.enabled) {
            Log.i("AlarmReceiver", "自动任务已关闭，忽略本次闹钟")
            return
        }

        if (!ChineseHolidayCalendar.isBusinessDay(Calendar.getInstance())) {
            Log.i("AlarmReceiver", "今天不是工作日，跳过本次闹钟并调度下一工作日")
            AlarmScheduler.scheduleNext(context, isClockIn)
            return
        }

        if (!ClockAttemptTracker.canAttempt(prefs, isClockIn)) {
            Log.w("AlarmReceiver", "今日${if (isClockIn) "开始" else "结束"}任务已达尝试上限，跳过本次闹钟")
            AlarmScheduler.scheduleNext(context, isClockIn)
            return
        }
        ClockAttemptTracker.incrementAttemptCount(prefs, isClockIn)

        val service = AutoClockService.instance
        if (service != null) {
            // 先安排下一工作日兜底；服务完成后若失败且仍可重试，会覆盖为重试闹钟。
            AlarmScheduler.scheduleNext(context, isClockIn)
            service.performClockSequence(isClockIn, scheduleAfterCompletion = true)
        } else {
            Log.w("AlarmReceiver", "AccessibilityService 未运行，请在系统设置中开启无障碍服务")
            recordMissingServiceAsync(context, isClockIn)
        }
    }

    private fun recordMissingServiceAsync(context: Context, isClockIn: Boolean) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        Thread {
            try {
                val prefs = Prefs(appContext)
                val record = missingServiceRecord(isClockIn, System.currentTimeMillis())
                prefs.clockHistoryJson = ClockHistory.append(prefs.clockHistoryJson, record)
                EmailSender.sendFailureEmail(appContext, record.reason)
                AlarmScheduler.scheduleRetryOrNext(appContext, isClockIn)
            } finally {
                pendingResult.finish()
            }
        }.start()
    }
}
