package com.autoclock

import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockSuccessDetectorTest {

    @Test
    fun `detects target app popup containing success text`() {
        val snapshot = snapshot(texts = listOf("✓", "操作成功"))

        assertTrue(ClockSuccessDetector.isSuccessPopup(snapshot))
    }

    @Test
    fun `detects success text from content description`() {
        val snapshot = snapshot(texts = emptyList(), contentDescription = "操作成功")

        assertTrue(ClockSuccessDetector.isSuccessPopup(snapshot))
    }

    @Test
    fun `does not treat neutral target app text as successful response`() {
        val snapshot = snapshot(texts = listOf("今天也要加油", "保持热爱"), eventType = CONTENT_CHANGED_EVENT)

        assertFalse(ClockSuccessDetector.isSuccessPopup(snapshot))
    }

    @Test
    fun `ignores other packages even when success text is present`() {
        val snapshot = snapshot(packageName = "com.android.launcher", texts = listOf("操作成功"))

        assertFalse(ClockSuccessDetector.isSuccessPopup(snapshot))
    }

    @Test
    fun `ignores failure popup text`() {
        val snapshot = snapshot(texts = listOf("任务失败", "请稍后重试"), eventType = CONTENT_CHANGED_EVENT)

        assertFalse(ClockSuccessDetector.isSuccessPopup(snapshot))
    }

    @Test
    fun `ignores unsuccessful response text`() {
        val snapshot = snapshot(texts = listOf("打卡不成功", "请稍后重试"), eventType = CONTENT_CHANGED_EVENT)

        assertFalse(ClockSuccessDetector.isSuccessPopup(snapshot))
        assertTrue(ClockSuccessDetector.isFailurePopup(snapshot))
    }

    @Test
    fun `prefers explicit success over stale failure text in the same target window`() {
        val snapshot = snapshot(texts = listOf("上次任务失败", "操作成功"), eventType = CONTENT_CHANGED_EVENT)

        assertTrue(ClockSuccessDetector.isSuccessPopup(snapshot))
    }

    @Test
    fun `detects success text in noisy target app window text`() {
        val snapshot = snapshot(texts = listOf("目标 App", "示例", "✓", "操作成功", "知道了"))

        assertTrue(ClockSuccessDetector.isSuccessPopup(snapshot))
    }

    @Test
    fun `detects content changed event containing success text`() {
        val snapshot = snapshot(texts = listOf("操作成功"), eventType = CONTENT_CHANGED_EVENT)

        assertTrue(ClockSuccessDetector.isSuccessPopup(snapshot))
    }

    @Test
    fun `detects success text from active window polling text`() {
        val snapshot = snapshot(texts = listOf("目标 App", "操作成功"), eventType = 0)

        assertTrue(ClockSuccessDetector.isSuccessPopup(snapshot))
    }

    @Test
    fun `does not treat available clock range text as success before tapping`() {
        val snapshot = snapshot(
            className = TargetApps.SMART_ATTEND_ACTIVITY,
            texts = listOf("下班1", "18:00", "打卡时间范围 13:31-次日04:59"),
            eventType = CONTENT_CHANGED_EVENT
        )

        assertEquals(ClockResponseDecision.UNKNOWN, ClockSuccessDetector.detectResponse(snapshot))
    }

    @Test
    fun `does not treat target app window state change as explicit text success`() {
        val snapshot = snapshot(texts = emptyList(), eventType = WINDOW_STATE_CHANGED_EVENT)

        assertFalse(ClockSuccessDetector.isSuccessPopup(snapshot))
    }

    @Test
    fun `detects target app dialog window state change as successful response decision`() {
        val snapshot = snapshot(texts = emptyList(), eventType = WINDOW_STATE_CHANGED_EVENT)

        assertEquals(ClockResponseDecision.SUCCESS, ClockSuccessDetector.detectResponse(snapshot))
    }

    @Test
    fun `target app attendance activity window state change remains unknown without response text`() {
        val snapshot = snapshot(
            className = TargetApps.SMART_ATTEND_ACTIVITY,
            texts = emptyList(),
            eventType = WINDOW_STATE_CHANGED_EVENT
        )

        assertEquals(ClockResponseDecision.UNKNOWN, ClockSuccessDetector.detectResponse(snapshot))
    }

    @Test
    fun `target app ordinary activity window state change remains unknown without response text`() {
        val snapshot = snapshot(
            className = "com.kdweibo.client.SomeIntermediateActivity",
            texts = emptyList(),
            eventType = WINDOW_STATE_CHANGED_EVENT
        )

        assertEquals(ClockResponseDecision.UNKNOWN, ClockSuccessDetector.detectResponse(snapshot))
    }

    @Test
    fun `active window polling failure text without popup surface remains unknown`() {
        val snapshot = snapshot(
            className = null,
            texts = listOf("上次任务失败"),
            eventType = 0
        )

        assertEquals(ClockResponseDecision.UNKNOWN, ClockSuccessDetector.detectResponse(snapshot))
    }

    @Test
    fun `target app window state change with explicit failure text is failure decision`() {
        val snapshot = snapshot(texts = listOf("打卡不成功", "请稍后重试"), eventType = WINDOW_STATE_CHANGED_EVENT)

        assertEquals(ClockResponseDecision.FAILURE, ClockSuccessDetector.detectResponse(snapshot))
    }

    @Test
    fun `content changed event with explicit failure text is failure decision`() {
        val snapshot = snapshot(texts = listOf("打卡不成功", "请稍后重试"), eventType = CONTENT_CHANGED_EVENT)

        assertEquals(ClockResponseDecision.FAILURE, ClockSuccessDetector.detectResponse(snapshot))
    }

    @Test
    fun `content changed event with explicit success text is success decision`() {
        val snapshot = snapshot(texts = listOf("上次任务失败", "操作成功"), eventType = CONTENT_CHANGED_EVENT)

        assertEquals(ClockResponseDecision.SUCCESS, ClockSuccessDetector.detectResponse(snapshot))
    }

    @Test
    fun `neutral content changed target app text remains unknown decision`() {
        val snapshot = snapshot(texts = listOf("今天也要加油", "保持热爱"), eventType = CONTENT_CHANGED_EVENT)

        assertEquals(ClockResponseDecision.UNKNOWN, ClockSuccessDetector.detectResponse(snapshot))
    }

    @Test
    fun `ignores blank package with success text`() {
        val snapshot = snapshot(packageName = null, texts = listOf("操作成功"))

        assertFalse(ClockSuccessDetector.isSuccessPopup(snapshot))
    }

    @Test
    fun `does not treat neutral target content description as successful response`() {
        val snapshot = snapshot(texts = emptyList(), contentDescription = "今天也要加油", eventType = CONTENT_CHANGED_EVENT)

        assertFalse(ClockSuccessDetector.isSuccessPopup(snapshot))
    }

    @Test
    fun `detects target app window state changed event as clock response window`() {
        assertTrue(ClockSuccessDetector.isClockResponseWindow(ClockSuccessDetector.CLOCK_APP_PACKAGE, WINDOW_STATE_CHANGED_EVENT))
    }

    @Test
    fun `ignores target app non window state changed event as clock response window`() {
        assertFalse(ClockSuccessDetector.isClockResponseWindow(ClockSuccessDetector.CLOCK_APP_PACKAGE, CONTENT_CHANGED_EVENT))
    }

    @Test
    fun `ignores non target package window state changed event as clock response window`() {
        assertFalse(ClockSuccessDetector.isClockResponseWindow("com.android.launcher", WINDOW_STATE_CHANGED_EVENT))
    }

    private fun snapshot(
        packageName: String? = ClockSuccessDetector.CLOCK_APP_PACKAGE,
        className: String? = "android.app.Dialog",
        texts: List<String>,
        contentDescription: String? = null,
        eventType: Int = WINDOW_STATE_CHANGED_EVENT
    ): ClockAccessibilitySnapshot {
        return ClockAccessibilitySnapshot(
            packageName = packageName,
            className = className,
            texts = texts,
            contentDescription = contentDescription,
            eventType = eventType
        )
    }

    private companion object {
        const val WINDOW_STATE_CHANGED_EVENT = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        const val CONTENT_CHANGED_EVENT = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
    }
}
