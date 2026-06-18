package com.autoclock

import android.view.accessibility.AccessibilityEvent

enum class ClockResponseDecision {
    SUCCESS,
    FAILURE,
    UNKNOWN
}

object ClockSuccessDetector {
    const val CLOCK_APP_PACKAGE = TargetApps.CLOCK_PACKAGE

    private val successKeywords = listOf("操作成功", "打卡成功", "已打卡", "已签到")
    private val failureKeywords = listOf("失败", "不成功", "请稍后重试", "未在前台")
    private val responseWindowClassKeywords = listOf("Dialog", "Popup", "Toast")

    /**
     * 主检测：TYPE_WINDOW_STATE_CHANGED 来自目标 App = 可能出现打卡响应窗口。
     * 真正的无文案成功只接受弹窗类窗口，避免普通 Activity 跳转误判成功。
     */
    fun isClockResponseWindow(event: AccessibilityEvent): Boolean {
        return isClockResponseWindow(event.packageName?.toString(), event.eventType)
    }

    fun isClockResponseWindow(packageName: String?, eventType: Int): Boolean {
        if (packageName != CLOCK_APP_PACKAGE) return false
        return eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
    }

    fun detectResponse(snapshot: ClockAccessibilitySnapshot): ClockResponseDecision {
        val isResponseSurface = isResponseSurface(snapshot)
        return when {
            isSuccessPopup(snapshot) -> ClockResponseDecision.SUCCESS
            isResponseSurface && isFailurePopup(snapshot) -> ClockResponseDecision.FAILURE
            isResponseWindowEvent(snapshot) -> ClockResponseDecision.SUCCESS
            else -> ClockResponseDecision.UNKNOWN
        }
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

    private fun isResponseWindowEvent(snapshot: ClockAccessibilitySnapshot): Boolean {
        return isClockResponseWindow(snapshot.packageName, snapshot.eventType) && isResponseSurface(snapshot)
    }

    private fun isResponseSurface(snapshot: ClockAccessibilitySnapshot): Boolean {
        if (snapshot.packageName != CLOCK_APP_PACKAGE) return false
        val className = snapshot.className?.trim()?.takeIf { it.isNotBlank() } ?: return false
        if (className == TargetApps.SMART_ATTEND_ACTIVITY) return false
        return responseWindowClassKeywords.any { keyword -> className.contains(keyword, ignoreCase = true) }
    }

    private fun candidates(snapshot: ClockAccessibilitySnapshot): List<String>? {
        if (snapshot.packageName != CLOCK_APP_PACKAGE) return null

        val candidates = (snapshot.texts + listOfNotNull(snapshot.contentDescription))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return candidates.ifEmpty { null }
    }
}
