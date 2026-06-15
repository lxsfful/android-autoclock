package com.autoclock

data class ForegroundCheckDecision(
    val isTargetReady: Boolean,
    val failureReason: String?
)

object ForegroundAppChecker {
    const val UNREADABLE_REASON = "无法确认当前前台 App，请检查无障碍服务和目标包名配置"
    const val NOT_FOREGROUND_REASON = "任务 App 未在前台，请检查桌面快捷方式坐标、目标包名和等待秒数"

    fun evaluate(observedPackages: List<String?>, targetPackage: String): ForegroundCheckDecision {
        if (observedPackages.any { it == targetPackage }) {
            return ForegroundCheckDecision(isTargetReady = true, failureReason = null)
        }

        val hasReadablePackage = observedPackages.any { !it.isNullOrBlank() }
        val reason = if (hasReadablePackage) NOT_FOREGROUND_REASON else UNREADABLE_REASON
        return ForegroundCheckDecision(isTargetReady = false, failureReason = reason)
    }
}
