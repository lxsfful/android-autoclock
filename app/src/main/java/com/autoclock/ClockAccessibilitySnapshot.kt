package com.autoclock

data class ClockAccessibilitySnapshot(
    val packageName: String?,
    val className: String?,
    val texts: List<String>,
    val contentDescription: String?,
    val eventType: Int
)
