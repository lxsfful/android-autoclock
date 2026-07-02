package com.autoclock

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class Prefs(context: Context) {

    private companion object {
        const val TAG = "Prefs"
        const val EMAIL_PASSWORD_KEY = "email_password"
        const val EMAIL_PASSWORD_SECURE_KEY = "email_password_secure"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val EMAIL_KEY_ALIAS = "AutoClockEmailCredentialKey"
        const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
    }

    private val sp: SharedPreferences =
        context.getSharedPreferences("autoclock_prefs", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = sp.getBoolean("enabled", false)
        set(v) = sp.edit().putBoolean("enabled", v).apply()

    // ---- 坐标：第一次点击（桌面快捷方式） ----
    var openAppX: Float
        get() = sp.getFloat("open_app_x", 0.08f)
        set(v) = sp.edit().putFloat("open_app_x", v).apply()

    var openAppY: Float
        get() = sp.getFloat("open_app_y", 0.92f)
        set(v) = sp.edit().putFloat("open_app_y", v).apply()

    // ---- 坐标：第二次点击（任务按钮） ----
    var clockBtnX: Float
        get() = sp.getFloat("clock_btn_x", 0.50f)
        set(v) = sp.edit().putFloat("clock_btn_x", v).apply()

    var clockBtnY: Float
        get() = sp.getFloat("clock_btn_y", 0.22f)
        set(v) = sp.edit().putFloat("clock_btn_y", v).apply()

    var waitSeconds: Int
        get() = sp.getInt("wait_seconds", 4)
        set(v) = sp.edit().putInt("wait_seconds", v).apply()

    // ---- 坐标：第三次点击（任务完成后，桌面快捷方式） ----
    var afterClockX: Float
        get() = sp.getFloat("after_clock_x", 0.92f)
        set(v) = sp.edit().putFloat("after_clock_x", v).apply()

    var afterClockY: Float
        get() = sp.getFloat("after_clock_y", 0.92f)
        set(v) = sp.edit().putFloat("after_clock_y", v).apply()

    // ---- 任务时间窗口 ----
    var clockInStartHour: Int
        get() = sp.getInt("ci_start_h", 8)
        set(v) = sp.edit().putInt("ci_start_h", v).apply()

    var clockInStartMinute: Int
        get() = sp.getInt("ci_start_m", 30)
        set(v) = sp.edit().putInt("ci_start_m", v).apply()

    var clockInEndHour: Int
        get() = sp.getInt("ci_end_h", 8)
        set(v) = sp.edit().putInt("ci_end_h", v).apply()

    var clockInEndMinute: Int
        get() = sp.getInt("ci_end_m", 40)
        set(v) = sp.edit().putInt("ci_end_m", v).apply()

    var clockOutStartHour: Int
        get() = sp.getInt("co_start_h", 18)
        set(v) = sp.edit().putInt("co_start_h", v).apply()

    var clockOutStartMinute: Int
        get() = sp.getInt("co_start_m", 20)
        set(v) = sp.edit().putInt("co_start_m", v).apply()

    var clockOutEndHour: Int
        get() = sp.getInt("co_end_h", 18)
        set(v) = sp.edit().putInt("co_end_h", v).apply()

    var clockOutEndMinute: Int
        get() = sp.getInt("co_end_m", 30)
        set(v) = sp.edit().putInt("co_end_m", v).apply()

    // ---- 已调度的下次触发时间（毫秒时间戳，用于主界面展示） ----
    var nextClockInTime: Long
        get() = sp.getLong("next_ci_time", 0L)
        set(v) = sp.edit().putLong("next_ci_time", v).apply()

    var nextClockOutTime: Long
        get() = sp.getLong("next_co_time", 0L)
        set(v) = sp.edit().putLong("next_co_time", v).apply()

    // ---- 每日尝试次数（用于同一早/晚窗口最多两次） ----
    var clockInAttemptDate: String
        get() = sp.getString("ci_attempt_date", "") ?: ""
        set(v) = sp.edit().putString("ci_attempt_date", v).apply()

    var clockInAttemptCount: Int
        get() = sp.getInt("ci_attempt_count", 0)
        set(v) = sp.edit().putInt("ci_attempt_count", v).apply()

    var clockOutAttemptDate: String
        get() = sp.getString("co_attempt_date", "") ?: ""
        set(v) = sp.edit().putString("co_attempt_date", v).apply()

    var clockOutAttemptCount: Int
        get() = sp.getInt("co_attempt_count", 0)
        set(v) = sp.edit().putInt("co_attempt_count", v).apply()

    // ---- 任务历史记录 ----
    var clockHistoryJson: String
        get() = sp.getString("clock_history_json", "") ?: ""
        set(v) = sp.edit().putString("clock_history_json", v).apply()

    // ---- 邮件通知配置 ----
    var emailSmtpHost: String
        get() = sp.getString("email_smtp_host", "smtp.example.com") ?: "smtp.example.com"
        set(v) = sp.edit().putString("email_smtp_host", v).apply()

    var emailSmtpPort: Int
        get() = sp.getInt("email_smtp_port", 465)
        set(v) = sp.edit().putInt("email_smtp_port", v).apply()

    var emailSender: String
        get() = sp.getString("email_sender", "") ?: ""
        set(v) = sp.edit().putString("email_sender", v).apply()

    var emailPassword: String
        get() = readSecureEmailPassword()
        set(v) {
            writeSecureEmailPassword(v)
        }

    fun saveEmailPassword(value: String): Boolean {
        return writeSecureEmailPassword(value)
    }

    /** 收件邮箱，留空则与发件箱相同 */
    var emailRecipient: String
        get() = sp.getString("email_recipient", "") ?: ""
        set(v) = sp.edit().putString("email_recipient", v).apply()

    private fun readSecureEmailPassword(): String {
        val encrypted = sp.getString(EMAIL_PASSWORD_SECURE_KEY, null)
        if (!encrypted.isNullOrBlank()) {
            return decryptString(encrypted) ?: ""
        }

        val plaintext = sp.getString(EMAIL_PASSWORD_KEY, "") ?: ""
        if (plaintext.isBlank()) return ""

        return if (writeSecureEmailPassword(plaintext)) {
            plaintext
        } else {
            Log.e(TAG, "旧版明文邮件授权码迁移失败，已停止使用明文值")
            ""
        }
    }

    private fun writeSecureEmailPassword(value: String): Boolean {
        val editor = sp.edit().remove(EMAIL_PASSWORD_KEY)
        if (value.isBlank()) {
            editor.remove(EMAIL_PASSWORD_SECURE_KEY).apply()
            return true
        }

        val encrypted = encryptString(value)
        if (encrypted == null) {
            editor.remove(EMAIL_PASSWORD_SECURE_KEY).apply()
            Log.e(TAG, "邮件授权码加密失败，已清除旧版明文值")
            return false
        }
        editor.putString(EMAIL_PASSWORD_SECURE_KEY, encrypted).apply()
        return true
    }

    private fun encryptString(value: String): String? {
        return runCatching {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
            val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            val payload = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            "$iv.$payload"
        }.onFailure { error ->
            Log.e(TAG, "加密邮件授权码失败", error)
        }.getOrNull()
    }

    private fun decryptString(value: String): String? {
        return runCatching {
            val parts = value.split('.', limit = 2)
            if (parts.size != 2) return@runCatching null

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
        }.onFailure { error ->
            Log.e(TAG, "解密邮件授权码失败", error)
        }.getOrNull()
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existingKey = keyStore.getEntry(EMAIL_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existingKey != null) return existingKey.secretKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            EMAIL_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
