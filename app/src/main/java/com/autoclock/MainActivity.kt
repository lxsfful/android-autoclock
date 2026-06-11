package com.autoclock

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.autoclock.databinding.ActivityMainBinding
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private companion object {
        const val MIN_PERCENT = 0
        const val MAX_PERCENT = 100
        const val MIN_WAIT_SECONDS = 1
        const val MAX_WAIT_SECONDS = 60
        const val TIME_PICKER_TAG = "clock_time_picker"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs(this)

        setupSwitch()
        setupButtons()
        setupCoordinateInputs()
        setupTimeSection()
        setupEmailSection()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        refreshHistory()
        setupCoordinateInputs()
        setupTimeSection()
        binding.switchEnable.isChecked = prefs.enabled
    }

    // ---- 主开关 ----

    private fun setupSwitch() {
        binding.switchEnable.isChecked = prefs.enabled
        binding.switchEnable.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                when {
                    !isAccessibilityEnabled() -> {
                        toast("请先开启无障碍服务（见下方按钮）")
                        binding.switchEnable.isChecked = false
                        return@setOnCheckedChangeListener
                    }
                    !canScheduleExactAlarms() -> {
                        toast("请授权「精确闹钟」权限")
                        requestExactAlarmPermission()
                        binding.switchEnable.isChecked = false
                        return@setOnCheckedChangeListener
                    }
                    else -> {
                        prefs.enabled = true
                        AlarmScheduler.scheduleAll(this)
                        toast("自动任务已启用")
                    }
                }
            } else {
                prefs.enabled = false
                AlarmScheduler.cancelAll(this)
                toast("自动任务已关闭")
            }
            refreshStatus()
        }
    }

    // ---- 按钮 ----

    private fun setupButtons() {
        binding.btnUsageGuide.setOnClickListener {
            startActivity(Intent(this, UsageGuideActivity::class.java))
        }
        binding.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        binding.btnTestIn.setOnClickListener {
            AutoClockService.instance?.performClockSequence(true)
                ?: toast("无障碍服务未运行，请在系统设置中开启")
        }
        binding.btnTestOut.setOnClickListener {
            AutoClockService.instance?.performClockSequence(false)
                ?: toast("无障碍服务未运行，请在系统设置中开启")
        }
        binding.btnPickOpenCoords.setOnClickListener {
            openCoordinatePicker(CoordinatePickerActivity.TARGET_OPEN_APP)
        }
        binding.btnPickClockCoords.setOnClickListener {
            openCoordinatePicker(CoordinatePickerActivity.TARGET_CLOCK_BUTTON)
        }
        binding.btnPickAfterClockCoords.setOnClickListener {
            openCoordinatePicker(CoordinatePickerActivity.TARGET_AFTER_CLOCK)
        }
        binding.btnSaveCoords.setOnClickListener { saveCoordinates() }
    }

    private fun openCoordinatePicker(target: String) {
        startActivity(
            Intent(this, CoordinatePickerActivity::class.java)
                .putExtra(CoordinatePickerActivity.EXTRA_TARGET, target)
        )
    }

    // ---- 坐标输入 ----

    private fun setupCoordinateInputs() {
        binding.etOpenX.setText(formatPercent(prefs.openAppX))
        binding.etOpenY.setText(formatPercent(prefs.openAppY))
        binding.etClockX.setText(formatPercent(prefs.clockBtnX))
        binding.etClockY.setText(formatPercent(prefs.clockBtnY))
        binding.etAfterClockX.setText(formatPercent(prefs.afterClockX))
        binding.etAfterClockY.setText(formatPercent(prefs.afterClockY))
        binding.etWait.setText(prefs.waitSeconds.toString())
    }

    private fun saveCoordinates() {
        val openAppX    = parsePercentInput(binding.etOpenX.text.toString(),       "打开快捷方式 X 坐标") ?: return
        val openAppY    = parsePercentInput(binding.etOpenY.text.toString(),       "打开快捷方式 Y 坐标") ?: return
        val clockBtnX   = parsePercentInput(binding.etClockX.text.toString(),      "任务按钮 X 坐标")    ?: return
        val clockBtnY   = parsePercentInput(binding.etClockY.text.toString(),      "任务按钮 Y 坐标")    ?: return
        val afterClockX = parsePercentInput(binding.etAfterClockX.text.toString(), "任务后快捷方式 X 坐标") ?: return
        val afterClockY = parsePercentInput(binding.etAfterClockY.text.toString(), "任务后快捷方式 Y 坐标") ?: return
        val waitSecs    = parseWaitSeconds(binding.etWait.text.toString())         ?: return

        prefs.openAppX    = openAppX    / 100f
        prefs.openAppY    = openAppY    / 100f
        prefs.clockBtnX   = clockBtnX   / 100f
        prefs.clockBtnY   = clockBtnY   / 100f
        prefs.afterClockX = afterClockX / 100f
        prefs.afterClockY = afterClockY / 100f
        prefs.waitSeconds = waitSecs
        toast("坐标已保存")
    }

    // ---- 时间窗口配置 ----

    private fun setupTimeSection() {
        updateTimeButton(binding.btnClockInStart,  prefs.clockInStartHour,   prefs.clockInStartMinute)
        updateTimeButton(binding.btnClockInEnd,    prefs.clockInEndHour,     prefs.clockInEndMinute)
        updateTimeButton(binding.btnClockOutStart, prefs.clockOutStartHour,  prefs.clockOutStartMinute)
        updateTimeButton(binding.btnClockOutEnd,   prefs.clockOutEndHour,    prefs.clockOutEndMinute)

        binding.btnClockInStart.setOnClickListener {
            pickTime(prefs.clockInStartHour, prefs.clockInStartMinute) { h, m ->
                prefs.clockInStartHour = h; prefs.clockInStartMinute = m
                updateTimeButton(binding.btnClockInStart, h, m)
                rescheduleIfEnabled()
            }
        }
        binding.btnClockInEnd.setOnClickListener {
            pickTime(prefs.clockInEndHour, prefs.clockInEndMinute) { h, m ->
                prefs.clockInEndHour = h; prefs.clockInEndMinute = m
                updateTimeButton(binding.btnClockInEnd, h, m)
                rescheduleIfEnabled()
            }
        }
        binding.btnClockOutStart.setOnClickListener {
            pickTime(prefs.clockOutStartHour, prefs.clockOutStartMinute) { h, m ->
                prefs.clockOutStartHour = h; prefs.clockOutStartMinute = m
                updateTimeButton(binding.btnClockOutStart, h, m)
                rescheduleIfEnabled()
            }
        }
        binding.btnClockOutEnd.setOnClickListener {
            pickTime(prefs.clockOutEndHour, prefs.clockOutEndMinute) { h, m ->
                prefs.clockOutEndHour = h; prefs.clockOutEndMinute = m
                updateTimeButton(binding.btnClockOutEnd, h, m)
                rescheduleIfEnabled()
            }
        }
    }

    private fun pickTime(hour: Int, minute: Int, onPicked: (Int, Int) -> Unit) {
        if (supportFragmentManager.findFragmentByTag(TIME_PICKER_TAG) != null) return

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setTitleText("选择时间（24小时制）")
            .setHour(hour)
            .setMinute(minute)
            .build()
        picker.addOnPositiveButtonClickListener {
            onPicked(picker.hour, picker.minute)
        }
        picker.show(supportFragmentManager, TIME_PICKER_TAG)
    }

    private fun updateTimeButton(btn: android.widget.Button, hour: Int, minute: Int) {
        btn.text = formatTimeLabel(hour, minute)
    }

    private fun rescheduleIfEnabled() {
        if (prefs.enabled) {
            AlarmScheduler.scheduleAll(this)
            refreshStatus()
        }
    }

    // ---- 邮件配置 ----

    private fun setupEmailSection() {
        binding.etSmtpHost.setText(prefs.emailSmtpHost)
        binding.etSmtpPort.setText(prefs.emailSmtpPort.toString())
        binding.etEmailSender.setText(prefs.emailSender)
        binding.etEmailPassword.setText("")
        binding.etEmailRecipient.setText(prefs.emailRecipient)

        binding.btnSaveEmail.setOnClickListener { saveEmailConfig() }
        binding.btnTestEmail.setOnClickListener { sendTestEmail() }
    }

    private fun saveEmailConfig(): Boolean {
        val host      = binding.etSmtpHost.text.toString().trim()
        val portStr   = binding.etSmtpPort.text.toString().trim()
        val sender    = binding.etEmailSender.text.toString().trim()
        val password  = binding.etEmailPassword.text.toString()
        val recipient = binding.etEmailRecipient.text.toString().trim()

        val port = portStr.toIntOrNull()
        if (host.isBlank() || port == null || sender.isBlank()) {
            toast("SMTP 服务器、端口、发件邮箱不能为空")
            return false
        }
        if (port !in 1..65_535) {
            toast("SMTP 端口请输入 1–65535 的数字")
            return false
        }
        if (password.isBlank() && prefs.emailPassword.isBlank()) {
            toast("首次配置时 SMTP 授权码不能为空")
            return false
        }

        if (password.isNotBlank() && !prefs.saveEmailPassword(password)) {
            toast("SMTP 授权码加密保存失败，请重新填写")
            return false
        }

        prefs.emailSmtpHost   = host
        prefs.emailSmtpPort   = port
        prefs.emailSender     = sender
        prefs.emailRecipient  = recipient
        toast("邮件设置已保存")
        return true
    }

    private fun sendTestEmail() {
        if (!saveEmailConfig()) return
        if (prefs.emailSender.isBlank() || prefs.emailPassword.isBlank()) return
        toast("正在发送测试邮件…")
        EmailSender.sendTestEmail(this) { _, msg ->
            runOnUiThread { toast(msg) }
        }
    }

    // ---- 状态展示 ----

    private fun refreshStatus() {
        val a11yOk = isAccessibilityEnabled()
        val alarmOk = canScheduleExactAlarms()
        val active = prefs.enabled && a11yOk

        binding.tvAccessibilityStatus.text = if (a11yOk) "✅ 无障碍服务：已开启" else "❌ 无障碍服务：未开启"
        binding.tvAlarmStatus.text         = if (alarmOk) "✅ 精确闹钟权限：已授权" else "⚠️ 精确闹钟权限：未授权"
        binding.tvClockStatus.text         = if (active)  "✅ 自动任务：运行中" else "⏸ 自动任务：未启用"

        if (active) {
            val inTime  = prefs.nextClockInTime
            val outTime = prefs.nextClockOutTime
            binding.tvNextClockIn.text  = "🕗 下次开始任务：${formatTriggerTime(inTime)}"
            binding.tvNextClockOut.text = "🕕 下次结束任务：${formatTriggerTime(outTime)}"
            binding.tvNextClockIn.visibility  = View.VISIBLE
            binding.tvNextClockOut.visibility = View.VISIBLE
        } else {
            binding.tvNextClockIn.visibility  = View.GONE
            binding.tvNextClockOut.visibility = View.GONE
        }
    }

    private fun refreshHistory() {
        val records = ClockHistory.load(prefs.clockHistoryJson)
        binding.tvClockHistory.text = if (records.isEmpty()) {
            "（暂无记录）"
        } else {
            val formatter = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
            records.joinToString(separator = "\n") { formatHistoryRow(it, formatter) }
        }
    }

    private fun formatHistoryRow(record: ClockRecord, formatter: SimpleDateFormat): String {
        val dateText = formatter.format(Date(record.timestampMs))
        val typeText = if (record.isClockIn) "开始" else "结束"
        return if (record.success) {
            "✅ $dateText $typeText 操作成功"
        } else {
            "❌ $dateText $typeText 不成功：${record.reason.ifBlank { "原因未知" }}"
        }
    }

    private fun formatTriggerTime(ms: Long): String {
        if (ms == 0L) return "待调度"
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = ms }
        val timePart = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))
        return when {
            isSameDay(now, target) -> "今天 $timePart"
            isNextDay(now, target) -> "明天 $timePart"
            else -> {
                val dayPart = SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(ms))
                "$dayPart $timePart"
            }
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private fun isNextDay(a: Calendar, b: Calendar): Boolean {
        val next = a.clone() as Calendar
        next.add(Calendar.DAY_OF_YEAR, 1)
        return isSameDay(next, b)
    }

    // ---- 输入验证 ----

    private fun parsePercentInput(value: String, fieldName: String): Float? {
        val percent = value.trim().toFloatOrNull()
        if (percent == null || percent.isNaN() || percent < MIN_PERCENT || percent > MAX_PERCENT) {
            toast("${fieldName}请输入 $MIN_PERCENT–$MAX_PERCENT 的数字")
            return null
        }
        return percent
    }

    private fun parseWaitSeconds(value: String): Int? {
        val secs = value.trim().toIntOrNull()
        if (secs == null || secs !in MIN_WAIT_SECONDS..MAX_WAIT_SECONDS) {
            toast("等待秒数请输入 $MIN_WAIT_SECONDS–$MAX_WAIT_SECONDS 的整数")
            return null
        }
        return secs
    }

    // ---- 权限检查 ----

    private fun isAccessibilityEnabled(): Boolean {
        val service = "$packageName/${AutoClockService::class.java.canonicalName}"
        return try {
            val enabled = Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
            if (enabled == 1) {
                val services = Settings.Secure.getString(
                    contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
                services?.split(":")?.any { it.equals(service, ignoreCase = true) } ?: false
            } else false
        } catch (e: Exception) { false }
    }

    private fun canScheduleExactAlarms(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            (getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
        } else true
    }

    private fun requestExactAlarmPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        }
    }

    private fun formatPercent(ratio: Float): String {
        val percent = ratio * 100f
        if (percent.isNaN() || percent.isInfinite()) return ""
        return percent.toString().trimEnd('0').trimEnd('.')
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
