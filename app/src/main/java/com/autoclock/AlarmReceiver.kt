package com.autoclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

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

        val service = AutoClockService.instance
        if (service != null) {
            service.performClockSequence(isClockIn)
        } else {
            Log.w("AlarmReceiver", "AccessibilityService 未运行，请在系统设置中开启无障碍服务")
            recordMissingServiceAsync(context, isClockIn)
        }

        // 每次执行后重新调度下一次，保持循环
        AlarmScheduler.scheduleAll(context)
    }

    private fun recordMissingServiceAsync(context: Context, isClockIn: Boolean) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        Thread {
            try {
                val prefs = Prefs(appContext)
                val record = missingServiceRecord(isClockIn, System.currentTimeMillis())
                prefs.clockHistoryJson = ClockHistory.append(prefs.clockHistoryJson, record)
            } finally {
                pendingResult.finish()
            }
        }.start()
    }
}
