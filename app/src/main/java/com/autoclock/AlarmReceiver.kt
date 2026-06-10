package com.autoclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

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
        }

        // 每次执行后重新调度下一次，保持循环
        AlarmScheduler.scheduleAll(context)
    }
}
