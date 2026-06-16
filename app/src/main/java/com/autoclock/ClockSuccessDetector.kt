package com.autoclock

import android.view.accessibility.AccessibilityEvent

object ClockSuccessDetector {
    const val CLOCK_APP_PACKAGE = TargetApps.CLOCK_PACKAGE

    private val failureKeywords = listOf("失败", "不成功", "请稍后重试", "未在前台")

    /**
     * 主检测：TYPE_WINDOW_STATE_CHANGED 来自目标 App = 打卡响应弹窗出现。
     * 无论弹窗是"操作成功"还是心灵鸡汤，弹窗本身即代表打卡已被服务端处理。
     */
    fun isClockResponseWindow(event: AccessibilityEvent): Boolean {
        if (event.packageName?.toString() != CLOCK_APP_PACKAGE) return false
        return event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
    }

    /**
     * 次级检测（兜底）：文本中出现明确失败关键词则判失败；
     * 否则只要有文本内容就视为成功响应。
     */
    fun isSuccessPopup(snapshot: ClockAccessibilitySnapshot): Boolean {
        if (snapshot.packageName != CLOCK_APP_PACKAGE) return false

        val candidates = (snapshot.texts + listOfNotNull(snapshot.contentDescription))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (candidates.isEmpty()) return false

        // 有明确失败关键词 → 失败
        if (candidates.any { text -> failureKeywords.any { text.contains(it) } }) return false

        // 有任何文本内容 → 视为成功响应（操作成功 / 心灵鸡汤均覆盖）
        return true
    }
}
