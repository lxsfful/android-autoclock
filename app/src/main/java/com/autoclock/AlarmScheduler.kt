package com.autoclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar
import kotlin.random.Random

object AlarmScheduler {

    const val ACTION_CLOCK_IN  = "com.autoclock.CLOCK_IN"
    const val ACTION_CLOCK_OUT = "com.autoclock.CLOCK_OUT"

    private const val REQ_IN  = 100
    private const val REQ_OUT = 101

    fun scheduleAll(context: Context) {
        if (!Prefs(context).enabled) return
        schedule(context, clockIn = true)
        schedule(context, clockIn = false)
    }

    private fun schedule(context: Context, clockIn: Boolean) {
        val prefs = Prefs(context)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val hStart  = if (clockIn) prefs.clockInStartHour    else prefs.clockOutStartHour
        val mStart  = if (clockIn) prefs.clockInStartMinute  else prefs.clockOutStartMinute
        val hEnd    = if (clockIn) prefs.clockInEndHour      else prefs.clockOutEndHour
        val mEnd    = if (clockIn) prefs.clockInEndMinute    else prefs.clockOutEndMinute
        val reqCode = if (clockIn) REQ_IN  else REQ_OUT
        val action  = if (clockIn) ACTION_CLOCK_IN else ACTION_CLOCK_OUT

        // 窗口无效（end <= start）时跳过调度
        if (!AlarmWindow.isValid(hStart, mStart, hEnd, mEnd)) return

        val triggerAt = nextWeekdayTime(hStart, mStart, hEnd, mEnd)

        // 缓存下次触发时间供主界面展示
        if (clockIn) prefs.nextClockInTime = triggerAt
        else prefs.nextClockOutTime = triggerAt

        val pi = PendingIntent.getBroadcast(
            context, reqCode,
            Intent(context, AlarmReceiver::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    /**
     * 计算下一个工作日在 [hStart:mStart, hEnd:mEnd] 窗口内的随机时刻。
     * 如果今天的时间窗口已过，顺延到明天，再跳过周末。
     */
    private fun nextWeekdayTime(hStart: Int, mStart: Int, hEnd: Int, mEnd: Int): Long {
        val windowMs = ((hEnd - hStart) * 60L + (mEnd - mStart)) * 60_000L

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hStart)
            set(Calendar.MINUTE, mStart)
            set(Calendar.SECOND, Random.nextInt(0, 60))
            set(Calendar.MILLISECOND, 0)
        }
        cal.timeInMillis += Random.nextLong(windowMs)

        // 若今天的时间窗口已过（或正好已触发），顺延到明天
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, hStart)
            cal.set(Calendar.MINUTE, mStart)
            cal.set(Calendar.SECOND, Random.nextInt(0, 60))
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis += Random.nextLong(windowMs)
        }

        // 跳过周末
        while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
               cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // 保险：跳过周末后若时间仍在过去（极罕见），继续顺延到下一工作日
        while (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, hStart)
            cal.set(Calendar.MINUTE, mStart)
            cal.set(Calendar.SECOND, Random.nextInt(0, 60))
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis += Random.nextLong(windowMs)
            while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                   cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        return cal.timeInMillis
    }

    fun cancelAll(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(REQ_IN to ACTION_CLOCK_IN, REQ_OUT to ACTION_CLOCK_OUT).forEach { (req, action) ->
            val pi = PendingIntent.getBroadcast(
                context, req,
                Intent(context, AlarmReceiver::class.java).apply { this.action = action },
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pi?.let { am.cancel(it) }
        }
    }
}
