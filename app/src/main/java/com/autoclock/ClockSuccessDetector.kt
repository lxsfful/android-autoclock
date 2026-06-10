package com.autoclock

object ClockSuccessDetector {
    const val CLOCK_APP_PACKAGE = "com.example.targetapp"

    private const val SUCCESS_TEXT = "操作成功"

    fun isSuccessPopup(snapshot: ClockAccessibilitySnapshot): Boolean {
        if (snapshot.packageName != CLOCK_APP_PACKAGE) return false

        val searchableText = buildList {
            addAll(snapshot.texts)
            snapshot.contentDescription?.let { add(it) }
            snapshot.className?.let { add(it) }
        }.joinToString(separator = " ")

        return searchableText.contains(SUCCESS_TEXT)
    }
}
