package com.autoclock

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.KeyguardManager
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AutoClockService : AccessibilityService() {

    companion object {
        private const val TAG = "AutoClockService"
        private const val TAP_DURATION_MS = 80L
        private const val HOME_BEFORE_FIRST_TAP_DELAY_MS = 1_000L
        private const val HOME_SETTLE_DELAY_MS = 1_000L
        private const val SUCCESS_POPUP_TIMEOUT_MS = 8_000L
        private const val SUCCESS_POLL_INTERVAL_MS = 500L
        private const val SEQUENCE_WAKELOCK_SLACK_MS = 10_000L
        private const val FOREGROUND_RETRY_INTERVAL_MS = 500L
        private const val FOREGROUND_RETRY_TIMEOUT_MS = 5_000L
        private const val MIN_WAIT_SECONDS = 1
        private const val MAX_WAIT_SECONDS = 60
        private const val MAX_COLLECTED_NODE_TEXTS = 80
        private const val LOCKED_DEVICE_REASON = "设备处于锁屏状态，无法点击桌面快捷方式"

        // 打卡完成后额外操作的延迟
        private const val POST_CLOCK_DELAY_MS = 3_000L
        
        // 目标应用包名（云之家）
        private const val TARGET_APP_PACKAGE = TargetApps.CLOCK_PACKAGE
        // 向日葵包名
        private const val SUNFLOWER_PACKAGE = TargetApps.SUNFLOWER_PACKAGE

        /** AlarmReceiver 通过此静态引用调用任务序列 */
        var instance: AutoClockService? = null
    }

    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null
    private var isSequenceRunning = false
    private var isWaitingForSuccessPopup = false
    private var hasTerminalResult = false
    private var currentIsClockIn = true
    private var successPopupTimeoutRunnable: Runnable? = null
    private var successPopupPollRunnable: Runnable? = null
    private var lastWindowPackage: String? = null

    override fun onServiceConnected() {
        instance = this
        Log.i(TAG, "无障碍服务已连接")
    }

    override fun onDestroy() {
        instance = null
        handler.removeCallbacksAndMessages(null)
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        lastWindowPackage = event?.packageName?.toString()
        if (!isWaitingForSuccessPopup || hasTerminalResult || event == null) return
        if (lastWindowPackage != TARGET_APP_PACKAGE) return

        val snapshot = event.toClockSnapshot(collectActiveWindowTexts())
        if (ClockSuccessDetector.isSuccessPopup(snapshot)) {
            Log.i(TAG, "检测到云之家操作成功提示")
            completeSequence(success = true, reason = "检测到云之家操作成功提示")
        }
    }

    override fun onInterrupt() {}

    /**
     * 任务序列：
     *  1. 唤醒屏幕并回到桌面
     *  2. 点击桌面任务快捷方式
     *  3. 等待 waitSeconds 秒
     *  4. 点击任务按钮
     *  5. 等待目标 App「操作成功」弹窗
     *  6. 无论成功或失败，最后都回桌面并点击任务后快捷方式
     */
    fun performClockSequence(isClockIn: Boolean) {
        if (isSequenceRunning) {
            Log.w(TAG, "已有任务序列正在执行，忽略本次触发")
            return
        }

        Log.i(TAG, "开始任务序列: ${if (isClockIn) "开始" else "结束"}")
        isSequenceRunning = true
        currentIsClockIn = isClockIn
        isWaitingForSuccessPopup = false
        hasTerminalResult = false
        successPopupTimeoutRunnable = null
        successPopupPollRunnable = null
        lastWindowPackage = null

        val prefs = Prefs(this)
        val waitSeconds = prefs.waitSeconds.coerceIn(MIN_WAIT_SECONDS, MAX_WAIT_SECONDS)
        acquireWakeLock(sequenceWakeLockTimeoutMs(waitSeconds))
        if (isDeviceLocked()) {
            Log.w(TAG, LOCKED_DEVICE_REASON)
            completeSequence(success = false, reason = LOCKED_DEVICE_REASON, runPostSequence = false)
            return
        }
        val metrics = resources.displayMetrics
        val screenW = metrics.widthPixels.toFloat()
        val screenH = metrics.heightPixels.toFloat()

        // 先回到桌面，确保第一次点击命中桌面上的任务快捷方式
        returnToHomeScreen()
        handler.postDelayed({
            val x1 = tapCoordinate(screenW, prefs.openAppX)
            val y1 = tapCoordinate(screenH, prefs.openAppY)
            performTap(
                x1,
                y1,
                onCompleted = {
                    Log.d(TAG, "点击 #1 (打开 App 快捷方式) 已完成")
                    scheduleClockButtonTap(screenW, screenH, prefs, waitSeconds)
                },
                onCancelled = { completeSequence(success = false, reason = "打开任务 App 点击失败") }
            )
        }, HOME_BEFORE_FIRST_TAP_DELAY_MS)
    }

    private fun sequenceWakeLockTimeoutMs(waitSeconds: Int): Long {
        return HOME_BEFORE_FIRST_TAP_DELAY_MS +
            waitSeconds * 1000L +
            SUCCESS_POPUP_TIMEOUT_MS +
            2_000L +
            HOME_SETTLE_DELAY_MS +
            SEQUENCE_WAKELOCK_SLACK_MS
    }

    private fun scheduleClockButtonTap(screenW: Float, screenH: Float, prefs: Prefs, waitSeconds: Int) {
        val delayMs = waitSeconds * 1000L
        handler.postDelayed({
            waitForExpectedClockAppOpen {
                val x2 = tapCoordinate(screenW, prefs.clockBtnX)
                val y2 = tapCoordinate(screenH, prefs.clockBtnY)
                performTap(
                    x2,
                    y2,
                    onCompleted = {
                        Log.d(TAG, "点击 #2 (任务按钮) 已完成")
                        waitForSuccessPopup()
                    },
                    onCancelled = { completeSequence(success = false, reason = "任务按钮点击失败") }
                )
            }
        }, delayMs)
    }

    private fun waitForSuccessPopup() {
        isWaitingForSuccessPopup = true
        val poll = object : Runnable {
            override fun run() {
                if (!isWaitingForSuccessPopup || hasTerminalResult) return
                if (detectSuccessFromActiveWindow()) {
                    Log.i(TAG, "轮询检测到目标 App操作成功提示")
                    completeSequence(success = true, reason = "检测到目标 App操作成功提示")
                    return
                }
                handler.postDelayed(this, SUCCESS_POLL_INTERVAL_MS)
            }
        }
        val timeout = Runnable {
            completeSequence(success = false, reason = "等待目标 App操作成功提示超时")
        }
        successPopupPollRunnable = poll
        successPopupTimeoutRunnable = timeout
        handler.post(poll)
        handler.postDelayed(timeout, SUCCESS_POPUP_TIMEOUT_MS)
    }

    private fun detectSuccessFromActiveWindow(): Boolean {
        val texts = collectActiveWindowTexts()
        if (texts.isEmpty()) return false

        val snapshot = ClockAccessibilitySnapshot(
            packageName = TARGET_APP_PACKAGE,
            className = null,
            texts = texts,
            contentDescription = null,
            eventType = 0
        )
        return ClockSuccessDetector.isSuccessPopup(snapshot)
    }

    private fun completeSequence(success: Boolean, reason: String, runPostSequence: Boolean = true) {
        if (hasTerminalResult) return
        hasTerminalResult = true
        isWaitingForSuccessPopup = false
        successPopupTimeoutRunnable?.let { handler.removeCallbacks(it) }
        successPopupPollRunnable?.let { handler.removeCallbacks(it) }
        successPopupTimeoutRunnable = null
        successPopupPollRunnable = null

        if (success) {
            EmailSender.sendSuccessEmail(this)
        } else {
            EmailSender.sendFailureEmail(this, reason)
        }
        recordClockAttempt(success, reason)

        if (runPostSequence) {
            // 打卡完成后，延迟执行后续操作
            handler.postDelayed({ postClockSequence() }, POST_CLOCK_DELAY_MS)
        } else {
            releaseWakeLock()
        }
    }
    
    /**
     * 打卡完成后的操作序列：
     * 1. 返回桌面
     * 2. 杀死云之家进程
     * 3. 点击向日葵桌面快捷方式
     */
    private fun postClockSequence() {
        val prefs = Prefs(this)
        val metrics = resources.displayMetrics
        val screenW = metrics.widthPixels.toFloat()
        val screenH = metrics.heightPixels.toFloat()

        // 1. 返回桌面
        returnToHomeScreen()

        // 2. 延迟杀死云之家进程
        handler.postDelayed({
            killTargetApp()

            // 3. 延迟点击向日葵桌面快捷方式
            handler.postDelayed({
                val x = tapCoordinate(screenW, prefs.afterClockX)
                val y = tapCoordinate(screenH, prefs.afterClockY)
                performTap(x, y,
                    onCompleted = { releaseWakeLock() },
                    onCancelled = { releaseWakeLock() }
                )
            }, HOME_SETTLE_DELAY_MS)
        }, 500L)
    }

    private fun killTargetApp() {
        try {
            Runtime.getRuntime().exec(arrayOf("am", "force-stop", TARGET_APP_PACKAGE))
            Log.i(TAG, "已结束云之家进程: $TARGET_APP_PACKAGE")
        } catch (e: Exception) {
            Log.w(TAG, "结束云之家进程失败: $TARGET_APP_PACKAGE", e)
        }
    }
    
    private fun openSunflower() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(SUNFLOWER_PACKAGE)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                Log.i(TAG, "已打开向日葵: $SUNFLOWER_PACKAGE")
            } else {
                Log.w(TAG, "未找到向日葵应用: $SUNFLOWER_PACKAGE")
            }
        } catch (e: Exception) {
            Log.w(TAG, "打开向日葵失败: $SUNFLOWER_PACKAGE", e)
        }
    }

    private fun recordClockAttempt(success: Boolean, reason: String) {
        val appContext = applicationContext
        val record = ClockRecord(
            timestampMs = System.currentTimeMillis(),
            isClockIn = currentIsClockIn,
            success = success,
            reason = if (success) "" else reason
        )
        Thread {
            val prefs = Prefs(appContext)
            prefs.clockHistoryJson = ClockHistory.append(prefs.clockHistoryJson, record)
        }.start()
    }



    private fun waitForExpectedClockAppOpen(onReady: () -> Unit) {
        val observedPackages = mutableListOf<String?>()
        val maxAttempts = (FOREGROUND_RETRY_TIMEOUT_MS / FOREGROUND_RETRY_INTERVAL_MS).toInt() + 1

        fun check(attempt: Int) {
            if (hasTerminalResult) return

            val currentPackage = currentActiveWindowPackage()
            observedPackages.add(currentPackage)
            if (currentPackage == TARGET_APP_PACKAGE) {
                onReady()
                return
            }

            if (attempt >= maxAttempts) {
                val decision = ForegroundAppChecker.evaluate(observedPackages, TARGET_APP_PACKAGE)
                val reason = decision.failureReason ?: ForegroundAppChecker.NOT_FOREGROUND_REASON
                Log.w(TAG, reason)
                completeSequence(success = false, reason = reason)
                return
            }

            handler.postDelayed({ check(attempt + 1) }, FOREGROUND_RETRY_INTERVAL_MS)
        }

        check(attempt = 1)
    }

    /** 只读取当前窗口包名，用于点击前确认；不要在这里读取非目标 App 的节点文本。 */
    @Suppress("DEPRECATION")
    private fun currentActiveWindowPackage(): String? {
        val root = rootInActiveWindow ?: return null
        return try {
            root.packageName?.toString()
        } finally {
            root.recycle()
        }
    }

    private fun tapCoordinate(screenSize: Float, ratio: Float): Float {
        if (screenSize <= 1f || ratio.isNaN() || ratio.isInfinite()) {
            return 0f
        }

        val rawCoordinate = screenSize * ratio
        if (rawCoordinate.isNaN() || rawCoordinate.isInfinite()) {
            return 0f
        }

        return rawCoordinate.coerceIn(0f, screenSize - 1f)
    }

    private fun performTap(
        x: Float,
        y: Float,
        onCompleted: () -> Unit = {},
        onCancelled: () -> Unit = {}
    ) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, TAP_DURATION_MS)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        val isDispatched = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(TAG, "手势完成")
                onCompleted()
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.w(TAG, "手势取消")
                onCancelled()
            }
        }, null)
        if (!isDispatched) {
            Log.w(TAG, "手势派发失败")
            onCancelled()
        }
    }

    private fun returnToHomeScreen() {
        val isReturnedHome = performGlobalAction(GLOBAL_ACTION_HOME)
        if (isReturnedHome) {
            Log.i(TAG, "已执行返回桌面动作")
        } else {
            Log.w(TAG, "返回桌面失败")
        }
    }

    private fun AccessibilityEvent.toClockSnapshot(extraTexts: List<String>): ClockAccessibilitySnapshot {
        val eventTexts = text.mapNotNull { value ->
            value?.toString()?.trim()?.takeIf { it.isNotBlank() }
        }
        return ClockAccessibilitySnapshot(
            packageName = packageName?.toString(),
            className = className?.toString(),
            texts = (eventTexts + extraTexts).distinct(),
            contentDescription = contentDescription?.toString(),
            eventType = eventType
        )
    }

    /**
     * 读取当前窗口文本前必须再次校验包名，避免 canRetrieveWindowContent 读取非目标 App内容。
     */
    @Suppress("DEPRECATION")
    private fun collectActiveWindowTexts(): List<String> {
        val root = rootInActiveWindow ?: return emptyList()
        return try {
            if (root.packageName?.toString() != TARGET_APP_PACKAGE) {
                emptyList()
            } else {
                val texts = mutableListOf<String>()
                collectNodeTexts(root, texts)
                texts
            }
        } finally {
            root.recycle()
        }
    }

    @Suppress("DEPRECATION")
    private fun collectNodeTexts(node: AccessibilityNodeInfo, texts: MutableList<String>) {
        if (texts.size >= MAX_COLLECTED_NODE_TEXTS) return

        node.text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }

        for (index in 0 until node.childCount) {
            if (texts.size >= MAX_COLLECTED_NODE_TEXTS) return
            val child = node.getChild(index) ?: continue
            try {
                collectNodeTexts(child, texts)
            } finally {
                child.recycle()
            }
        }
    }

    private fun isDeviceLocked(): Boolean {
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as? KeyguardManager ?: return false
        if (!keyguardManager.isKeyguardSecure) return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            keyguardManager.isDeviceLocked || keyguardManager.isKeyguardLocked
        } else {
            keyguardManager.isKeyguardLocked
        }
    }

    @Suppress("DEPRECATION")
    private fun acquireWakeLock(timeoutMs: Long) {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        // SCREEN_BRIGHT_WAKE_LOCK + ACQUIRE_CAUSES_WAKEUP：在手机息屏时也能点亮屏幕
        // 虽已废弃，但从后台服务唤醒屏幕目前仍是最可靠的方式
        wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "AutoClock:ScreenWake"
        )
        wakeLock?.acquire(timeoutMs)
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null
        isSequenceRunning = false
        currentIsClockIn = true
        isWaitingForSuccessPopup = false
        hasTerminalResult = false
        successPopupTimeoutRunnable = null
        successPopupPollRunnable = null
    }
}
