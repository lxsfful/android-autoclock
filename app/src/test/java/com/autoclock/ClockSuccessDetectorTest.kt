package com.autoclock

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
    fun `ignores target app window without success text`() {
        val snapshot = snapshot(texts = listOf("目标 App", "示例任务"))

        assertFalse(ClockSuccessDetector.isSuccessPopup(snapshot))
    }

    @Test
    fun `ignores other packages even when success text is present`() {
        val snapshot = snapshot(packageName = "com.android.launcher", texts = listOf("操作成功"))

        assertFalse(ClockSuccessDetector.isSuccessPopup(snapshot))
    }

    @Test
    fun `ignores failure popup text`() {
        val snapshot = snapshot(texts = listOf("任务失败", "请稍后重试"))

        assertFalse(ClockSuccessDetector.isSuccessPopup(snapshot))
    }

    @Test
    fun `detects success when stale failure text is elsewhere in target window`() {
        val snapshot = snapshot(texts = listOf("上次任务失败", "操作成功"))

        assertTrue(ClockSuccessDetector.isSuccessPopup(snapshot))
    }

    @Test
    fun `detects success text in noisy target app window text`() {
        val snapshot = snapshot(texts = listOf("目标 App", "示例", "✓", "操作成功", "知道了"))

        assertTrue(ClockSuccessDetector.isSuccessPopup(snapshot))
    }

    @Test
    fun `detects content changed event containing success text`() {
        val snapshot = snapshot(texts = listOf("操作成功"), eventType = 2048)

        assertTrue(ClockSuccessDetector.isSuccessPopup(snapshot))
    }

    @Test
    fun `detects success text from active window polling text`() {
        val snapshot = snapshot(texts = listOf("目标 App", "操作成功"), eventType = 0)

        assertTrue(ClockSuccessDetector.isSuccessPopup(snapshot))
    }

    @Test
    fun `ignores target app window state change without success text`() {
        val snapshot = snapshot(texts = emptyList(), eventType = 32)

        assertFalse(ClockSuccessDetector.isSuccessPopup(snapshot))
    }

    @Test
    fun `ignores blank package with success text`() {
        val snapshot = snapshot(packageName = null, texts = listOf("操作成功"))

        assertFalse(ClockSuccessDetector.isSuccessPopup(snapshot))
    }

    private fun snapshot(
        packageName: String? = ClockSuccessDetector.CLOCK_APP_PACKAGE,
        texts: List<String>,
        contentDescription: String? = null,
        eventType: Int = 32
    ): ClockAccessibilitySnapshot {
        return ClockAccessibilitySnapshot(
            packageName = packageName,
            className = "android.app.Dialog",
            texts = texts,
            contentDescription = contentDescription,
            eventType = eventType
        )
    }
}
