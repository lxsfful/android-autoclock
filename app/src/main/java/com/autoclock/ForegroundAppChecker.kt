package com.autoclock

data class ForegroundCheckDecision(
    val isTargetReady: Boolean,
    val failureReason: String?
)

object ForegroundAppChecker {
    const val UNREADABLE_REASON = "无法确认当前前台 App，请检查无障碍服务和目标包名配置"
    const val NOT_FOREGROUND_REASON = "任务 App 未在前台，请检查桌面快捷方式坐标、目标包名和等待秒数"

    private const val MAX_DIAGNOSTIC_PACKAGES = 8

    fun evaluate(observedPackages: List<String?>, targetPackage: String): ForegroundCheckDecision {
        if (observedPackages.any { it == targetPackage }) {
            return ForegroundCheckDecision(isTargetReady = true, failureReason = null)
        }

        val hasReadablePackage = observedPackages.any { !it.isNullOrBlank() }
        val reason = if (hasReadablePackage) NOT_FOREGROUND_REASON else UNREADABLE_REASON
        return ForegroundCheckDecision(isTargetReady = false, failureReason = reason)
    }

    fun observedPackagesSummary(
        observedPackages: List<String?>,
        targetPackage: String = TargetApps.CLOCK_PACKAGE
    ): String {
        if (observedPackages.isEmpty()) return "无观测记录"

        val normalized = observedPackages.map { packageName -> packageCategory(packageName, targetPackage) }
        val limited = normalized.take(MAX_DIAGNOSTIC_PACKAGES)
        val suffix = if (normalized.size > MAX_DIAGNOSTIC_PACKAGES) listOf("...") else emptyList()
        return (limited + suffix).joinToString(" -> ")
    }

    private fun packageCategory(packageName: String?, targetPackage: String): String {
        val normalized = packageName?.trim()?.takeIf { it.isNotBlank() } ?: return "unreadable"
        return when {
            normalized == targetPackage -> "target"
            normalized == "com.autoclock" -> "autoclock"
            normalized.contains("launcher", ignoreCase = true) || normalized.contains("home", ignoreCase = true) -> "launcher"
            normalized == "android" || normalized.startsWith("com.android.") -> "system"
            else -> "other"
        }
    }

    fun diagnosticReason(
        observedPackages: List<String?>,
        targetPackage: String,
        expectedActivity: String,
        timeoutMs: Long,
        didRunSafetyTap: Boolean
    ): String {
        val decision = evaluate(observedPackages, targetPackage)
        val status = if (decision.isTargetReady) {
            "已确认任务 App 在前台"
        } else {
            decision.failureReason ?: NOT_FOREGROUND_REASON
        }
        val safetyTapStatus = if (didRunSafetyTap) "已继续执行保险点击" else "未执行保险点击"
        return "$status；目标包=$targetPackage；期望界面=$expectedActivity；等待=${timeoutMs}ms；观测包=${observedPackagesSummary(observedPackages, targetPackage)}；$safetyTapStatus"
    }
}
