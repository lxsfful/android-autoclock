package com.autoclock

import android.view.accessibility.AccessibilityEvent

object ClockSuccessDetector {
    const val CLOCK_APP_PACKAGE = "com.kdweibo.client"

    /**
     * 检测打卡成功：云之家弹出弹窗（打卡成功或鼓励鸡汤）
     * 只要检测到 TYPE_WINDOW_STATE_CHANGED 事件且包名是云之家，即视为打卡成功
     */
    fun isPopupWindow(event: AccessibilityEvent): Boolean {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return false
        if (event.packageName?.toString() != CLOCK_APP_PACKAGE) return false
        return true
    }
}
