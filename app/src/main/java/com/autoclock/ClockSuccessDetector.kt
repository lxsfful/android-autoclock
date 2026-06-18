package com.autoclock

import android.view.accessibility.AccessibilityEvent

object ClockSuccessDetector {
    const val CLOCK_APP_PACKAGE = TargetApps.CLOCK_PACKAGE

    private val successKeywords = listOf("操作成功", "打卡成功", "已打卡", "已签到")
    private val failureKeywords = listOf("失败", "不成功", "请稍后重试", "未在前台")

    /**
     * 主检测：TYPE_WINDOW_STATE_CHANGED 来自目标 App = 打卡响应弹窗出现。
     * 无论弹窗是"操作成功"还是心灵鸡汤，弹窗本身即代表打卡已被服务端处理。
     */
    fun isClockResponseWindow(event: AccessibilityEvent): Boolean {
        return isClockResponseWindow(event.packageName?.toString(), event.eventType)
    }

    fun isClockResponseWindow(packageName: String?, eventType: Int): Boolean {
        if (packageName != CLOCK_APP_PACKAGE) return false
        return eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
    }

    /**
     * 次级检测（兜底）：只在目标 App 文本中出现明确成功关键词时判成功。
     * 明确成功关键词优先于页面中的旧失败文案，避免“上次失败”等旧文案误杀本次成功。
     */
    fun isSuccessPopup(snapshot: ClockAccessibilitySnapshot): Boolean {
        val candidates = candidates(snapshot) ?: return false
        return candidates.any { text -> successKeywords.any { text.contains(it) } }
    }

    fun isFailurePopup(snapshot: ClockAccessibilitySnapshot): Boolean {
        val candidates = candidates(snapshot) ?: return false
        if (candidates.any { text -> successKeywords.any { text.contains(it) } }) return false
        return candidates.any { text -> failureKeywords.any { text.contains(it) } }
    }

    private fun candidates(snapshot: ClockAccessibilitySnapshot): List<String>? {
        if (snapshot.packageName != CLOCK_APP_PACKAGE) return null

        val candidates = (snapshot.texts + listOfNotNull(snapshot.contentDescription))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return candidates.ifEmpty { null }
    }
}
