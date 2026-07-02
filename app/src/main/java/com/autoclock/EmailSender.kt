package com.autoclock

import android.content.Context
import android.util.Log
import java.util.Properties
import java.util.concurrent.Executors
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailSender {

    private const val TAG = "EmailSender"
    private const val SMTP_TIMEOUT_MS = "15000"
    private val executor = Executors.newSingleThreadExecutor()

    fun sendClockResultEmail(context: Context, success: Boolean, reason: String = "") {
        if (!success) {
            Log.w(TAG, "任务不成功，准备发送提醒邮件: $reason")
        }
        val prefs = Prefs(context)
        val timeText = ClockEmailContent.formatClockTime(System.currentTimeMillis())
        sendAsync(
            host      = prefs.emailSmtpHost,
            port      = prefs.emailSmtpPort,
            sender    = prefs.emailSender,
            password  = prefs.emailPassword,
            recipient = effectiveRecipient(prefs),
            subject   = ClockEmailContent.resultSubject(success, timeText),
            body      = ClockEmailContent.resultBody(success, reason, timeText)
        )
    }

    fun sendSuccessEmail(context: Context) {
        sendClockResultEmail(context, success = true)
    }

    fun sendFailureEmail(context: Context, reason: String) {
        sendClockResultEmail(context, success = false, reason = reason)
    }

    fun sendTestEmail(context: Context, onResult: (success: Boolean, msg: String) -> Unit) {
        val prefs = Prefs(context)
        val host      = prefs.emailSmtpHost
        val port      = prefs.emailSmtpPort
        val sender    = prefs.emailSender
        val password  = prefs.emailPassword
        val recipient = effectiveRecipient(prefs)

        if (sender.isBlank() || password.isBlank()) {
            onResult(false, "发件邮箱或授权码未填写")
            return
        }

        executor.execute {
            try {
                send(host, port, sender, password, recipient,
                    subject = "AutoClock 邮件测试",
                    body    = "邮件配置验证成功，自动任务通知将使用此地址发送。")
                onResult(true, "测试邮件已发送至 $recipient")
            } catch (e: Exception) {
                logEmailFailure("测试邮件发送失败", e)
                onResult(false, "发送失败，请检查 SMTP 配置、授权码或网络")
            }
        }
    }

    private fun sendAsync(
        host: String, port: Int,
        sender: String, password: String, recipient: String,
        subject: String, body: String
    ) {
        if (sender.isBlank() || password.isBlank()) {
            Log.w(TAG, "邮件配置不完整，跳过发送")
            return
        }
        executor.execute {
            try {
                send(host, port, sender, password, recipient, subject, body)
                Log.i(TAG, "通知邮件已发送")
            } catch (e: Exception) {
                logEmailFailure("邮件发送失败", e)
            }
        }
    }

    private fun send(
        host: String, port: Int,
        sender: String, password: String, recipient: String,
        subject: String, body: String
    ) {
        val props = Properties().apply {
            put("mail.smtp.host", host)
            put("mail.smtp.port", port.toString())
            put("mail.smtp.auth", "true")
            put("mail.smtp.connectiontimeout", SMTP_TIMEOUT_MS)
            put("mail.smtp.timeout", SMTP_TIMEOUT_MS)
            put("mail.smtp.writetimeout", SMTP_TIMEOUT_MS)
            put("mail.smtp.ssl.checkserveridentity", "true")
            put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3")
            when (port) {
                587 -> {
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.starttls.required", "true")
                    put("mail.smtp.starttls.protocols", "TLSv1.2 TLSv1.3")
                }
                else -> {
                    // 465 是推荐 SSL 端口；其他非 587 端口也按 SSL 处理，避免明文降级。
                    put("mail.smtp.ssl.enable", "true")
                }
            }
        }
        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() =
                PasswordAuthentication(sender, password)
        })
        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(sender))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient))
            setSubject(subject, "UTF-8")
            setText(body, "UTF-8")
        }
        Transport.send(message)
    }

    private fun logEmailFailure(message: String, error: Exception) {
        Log.e(TAG, "$message: ${error.javaClass.simpleName}")
    }

    private fun effectiveRecipient(prefs: Prefs): String {
        val r = prefs.emailRecipient.trim()
        return if (r.isBlank()) prefs.emailSender else r
    }
}
