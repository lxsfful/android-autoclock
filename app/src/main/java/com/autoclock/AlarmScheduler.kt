package com.autoclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar
import kotlin.math.max
import kotlin.random.Random

object AlarmScheduler {

    const val ACTION_CLOCK_IN  = "com.autoclock.CLOCK_IN"
    const val ACTION_CLOCK_OUT = "com.autoclock.CLOCK_OUT"

    private const val TAG = "AlarmScheduler"
    private const val REQ_IN  = 100
    private const val REQ_OUT = 101
    private const val MIN_RETRY_DELAY_MS = 3L * 60L * 1000L

    internal enum class ScheduleMode {
        NORMAL,
        RETRY_WITHIN_CURRENT_WINDOW
    }

    internal data class ClockWindow(
        val hStart: Int,
        val mStart: Int,
        val hEnd: Int,
        val mEnd: Int
    )

    fun scheduleAll(context: Context) {
        if (!Prefs(context).enabled) return
        scheduleNext(context, clockIn = true)
        scheduleNext(context, clockIn = false)
    }

    fun scheduleNext(context: Context, clockIn: Boolean) {
        schedule(context, clockIn, ScheduleMode.NORMAL)
    }

    fun scheduleRetryOrNext(context: Context, clockIn: Boolean) {
        val prefs = Prefs(context)
        val attemptCount = ClockAttemptTracker.getAttemptCount(prefs, clockIn)
        schedule(context, clockIn, ScheduleMode.RETRY_WITHIN_CURRENT_WINDOW, attemptCount)
    }

    private fun schedule(
        context: Context,
        clockIn: Boolean,
        mode: ScheduleMode,
        attemptCount: Int = ClockAttemptTracker.MAX_ATTEMPTS_PER_WINDOW
    ) {
        val prefs = Prefs(context)
        if (!prefs.enabled) return

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val window = if (clockIn) {
            ClockWindow(
                prefs.clockInStartHour,
                prefs.clockInStartMinute,
                prefs.clockInEndHour,
                prefs.clockInEndMinute
            )
        } else {
            ClockWindow(
                prefs.clockOutStartHour,
                prefs.clockOutStartMinute,
                prefs.clockOutEndHour,
                prefs.clockOutEndMinute
            )
        }
        val reqCode = if (clockIn) REQ_IN else REQ_OUT
        val action = if (clockIn) ACTION_CLOCK_IN else ACTION_CLOCK_OUT

        val triggerAt = computeNextTrigger(
            nowMs = System.currentTimeMillis(),
            window = window,
            mode = mode,
            currentAttemptCount = attemptCount,
            randomOffsetProvider = { upperBound -> Random.nextLong(upperBound) }
        )

        if (triggerAt == null) {
            Log.w(TAG, "窗口无效，跳过调度: clockIn=$clockIn")
            cacheNextTrigger(prefs, clockIn, 0L)
            return
        }

        cacheNextTrigger(prefs, clockIn, triggerAt)

        val pi = PendingIntent.getBroadcast(
            context, reqCode,
            Intent(context, AlarmReceiver::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        Log.i(TAG, "已调度${if (clockIn) "开始" else "结束"}任务: triggerAt=$triggerAt mode=$mode")
    }

    internal fun computeNextTrigger(
        nowMs: Long,
        window: ClockWindow,
        mode: ScheduleMode,
        currentAttemptCount: Int = ClockAttemptTracker.MAX_ATTEMPTS_PER_WINDOW,
        randomOffsetProvider: (Long) -> Long = { upperBound -> Random.nextLong(upperBound) }
    ): Long? {
        if (!AlarmWindow.isValid(window.hStart, window.mStart, window.hEnd, window.mEnd)) return null

        if (mode == ScheduleMode.RETRY_WITHIN_CURRENT_WINDOW) {
            val retryAt = retryInsideCurrentWindow(nowMs, window, currentAttemptCount, randomOffsetProvider)
            if (retryAt != null) return retryAt
        }

        return nextNormalTrigger(nowMs, window, randomOffsetProvider)
    }

    private fun retryInsideCurrentWindow(
        nowMs: Long,
        window: ClockWindow,
        currentAttemptCount: Int,
        randomOffsetProvider: (Long) -> Long
    ): Long? {
        if (!ClockAttemptTracker.canAttempt(currentAttemptCount)) return null

        val now = Calendar.getInstance().apply { timeInMillis = nowMs }
        if (!ChineseHolidayCalendar.isBusinessDay(now)) return null

        val start = windowTime(now, window.hStart, window.mStart)
        val end = windowTime(now, window.hEnd, window.mEnd)
        if (nowMs < start.timeInMillis || nowMs >= end.timeInMillis) return null

        val earliestRetry = max(nowMs + MIN_RETRY_DELAY_MS, start.timeInMillis)
        val remainingMs = end.timeInMillis - earliestRetry
        if (remainingMs <= 0L) return null

        return earliestRetry + randomOffsetProvider(remainingMs)
    }

    private fun nextNormalTrigger(
        nowMs: Long,
        window: ClockWindow,
        randomOffsetProvider: (Long) -> Long
    ): Long {
        val now = Calendar.getInstance().apply { timeInMillis = nowMs }
        val todayStart = windowTime(now, window.hStart, window.mStart)
        val candidate = Calendar.getInstance().apply { timeInMillis = nowMs }

        if (ChineseHolidayCalendar.isBusinessDay(candidate) && nowMs < todayStart.timeInMillis) {
            return randomTimeInWindow(candidate, window, randomOffsetProvider)
        }

        candidate.add(Calendar.DAY_OF_MONTH, 1)
        while (!ChineseHolidayCalendar.isBusinessDay(candidate)) {
            candidate.add(Calendar.DAY_OF_MONTH, 1)
        }
        return randomTimeInWindow(candidate, window, randomOffsetProvider)
    }

    private fun randomTimeInWindow(
        day: Calendar,
        window: ClockWindow,
        randomOffsetProvider: (Long) -> Long
    ): Long {
        val start = windowTime(day, window.hStart, window.mStart)
        val end = windowTime(day, window.hEnd, window.mEnd)
        val windowMs = end.timeInMillis - start.timeInMillis
        return start.timeInMillis + randomOffsetProvider(windowMs)
    }

    private fun windowTime(sourceDay: Calendar, hour: Int, minute: Int): Calendar {
        return (sourceDay.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun cacheNextTrigger(prefs: Prefs, clockIn: Boolean, triggerAt: Long) {
        if (clockIn) prefs.nextClockInTime = triggerAt
        else prefs.nextClockOutTime = triggerAt
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
