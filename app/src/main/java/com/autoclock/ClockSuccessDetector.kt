package com.autoclock

import android.view.accessibility.AccessibilityEvent

object ClockSuccessDetector {
    const val CLOCK_APP_PACKAGE = TargetApps.CLOCK_PACKAGE

    private val successKeywords = listOf("操作成功")
    private val failureKeywords = listOf("失败", "不成功", "请稍后重试", "未在前台")

    /** 仅在目标 App 文本/内容描述中出现成功提示时判定成功。 */
    fun isSuccessPopup(snapshot: ClockAccessibilitySnapshot): Boolean {
        if (snapshot.packageName != CLOCK_APP_PACKAGE) return false

        val candidates = (snapshot.texts + listOfNotNull(snapshot.contentDescription))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (candidates.isEmpty()) return false

        return candidates.any { text ->
            successKeywords.any { text.contains(it) } &&
                failureKeywords.none { text.contains(it) }
        }
    }

    /** Android 事件入口保留为兼容薄封装；不能仅凭窗口变化事件判定成功。 */
    fun isPopupWindow(event: AccessibilityEvent): Boolean {
        val eventTexts = event.text.mapNotNull { value ->
            value?.toString()?.trim()?.takeIf { it.isNotBlank() }
        }
        val snapshot = ClockAccessibilitySnapshot(
            packageName = event.packageName?.toString(),
            className = event.className?.toString(),
            texts = eventTexts,
            contentDescription = event.contentDescription?.toString(),
            eventType = event.eventType
        )
        return isSuccessPopup(snapshot)
    }
}
