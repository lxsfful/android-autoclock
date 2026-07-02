package com.autoclock

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object DiagnosticRecorder {

    private const val TAG = "DiagnosticRecorder"
    private const val DIAGNOSTICS_DIR = "diagnostics"
    private const val MAX_STRING_LEN = 200
    private const val MAX_LIST_SIZE = 20
    private const val MAX_NODES = 120
    private const val MAX_DEPTH = 8
    private const val MAX_FILE_BYTES = 2 * 1024 * 1024L

    @Volatile
    private var recording = false

    @Volatile
    private var sessionId: String? = null

    @Volatile
    private var sessionStartMs = 0L

    @Volatile
    private var outputFile: File? = null

    private var writer: Writer? = null
    private var fileSizeBytes = 0L

    fun isRecording(): Boolean = recording

    fun latestDiagnosticFile(): File? = outputFile

    fun recordMarker(
        name: String,
        isClockIn: Boolean? = null,
        success: Boolean? = null,
        reason: String? = null
    ) {
        if (!recording) return

        val now = System.currentTimeMillis()
        val json = JSONObject()
            .put("type", "marker")
            .put("name", name)
            .put("sessionId", sessionId)
            .put("timestampMs", now)
            .put("elapsedMs", now - sessionStartMs)
        if (isClockIn != null) json.put("isClockIn", isClockIn)
        if (success != null) json.put("success", success)
        putOptional(json, "reason", reason?.let(::safeString))

        synchronized(this) {
            if (!recording) return
            runCatching {
                writeLineLocked(json)
                writer?.flush()
            }.onFailure { e ->
                Log.w(TAG, "Failed to write diagnostic marker", e)
            }
        }
    }

    @Synchronized
    fun startRecording(service: AccessibilityService): File? {
        if (recording) return outputFile

        return runCatching {
            val startMs = System.currentTimeMillis()
            val id = UUID.randomUUID().toString().take(8)
            val dir = File(service.filesDir, DIAGNOSTICS_DIR)
            if (!dir.exists() && !dir.mkdirs()) return null

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(startMs))
            val file = File(dir, "diagnostic_${timestamp}_$id.jsonl")
            val newWriter = file.outputStream().bufferedWriter()

            recording = true
            sessionId = id
            sessionStartMs = startMs
            outputFile = file
            writer = newWriter
            fileSizeBytes = 0L

            writeLineLocked(
                JSONObject()
                    .put("type", "session_start")
                    .put("sessionId", id)
                    .put("timestampMs", startMs)
                    .put("elapsedMs", 0L)
                    .put("packageName", service.packageName)
                    .put("appVersion", appVersion(service))
            )
            newWriter.flush()
            Log.i(TAG, "Diagnostic recording started: ${file.absolutePath}")
            file
        }.onFailure { e ->
            Log.e(TAG, "Failed to start diagnostic recording", e)
            closeWriter()
            recording = false
        }.getOrNull()
    }

    @Synchronized
    fun stopRecording(): File? {
        if (!recording) return null

        val file = outputFile
        runCatching {
            writeLineLocked(
                JSONObject()
                    .put("type", "session_stop")
                    .put("sessionId", sessionId)
                    .put("timestampMs", System.currentTimeMillis())
                    .put("elapsedMs", System.currentTimeMillis() - sessionStartMs)
                    .put("fileSizeBytes", fileSizeBytes)
            )
        }.onFailure { e ->
            Log.w(TAG, "Failed to write diagnostic stop marker", e)
        }

        closeWriter()
        recording = false
        sessionId = null
        sessionStartMs = 0L
        Log.i(TAG, "Diagnostic recording stopped: ${file?.absolutePath}")
        return file
    }

    fun recordEvent(service: AccessibilityService, event: AccessibilityEvent?) {
        if (!recording || event == null) return

        val now = System.currentTimeMillis()
        val json = runCatching { buildEventJson(service, event, now) }.getOrElse { e ->
            Log.w(TAG, "Failed to build diagnostic event", e)
            return
        }

        synchronized(this) {
            if (!recording) return
            if (fileSizeBytes >= MAX_FILE_BYTES) {
                stopRecording()
                return
            }
            runCatching {
                writeLineLocked(json)
                if (fileSizeBytes >= MAX_FILE_BYTES) {
                    stopRecording()
                } else if (fileSizeBytes % (64 * 1024L) < 4 * 1024L) {
                    writer?.flush()
                }
                Unit
            }.onFailure { e ->
                Log.e(TAG, "Failed to write diagnostic event", e)
            }
        }
    }

    private fun buildEventJson(
        service: AccessibilityService,
        event: AccessibilityEvent,
        timestampMs: Long
    ): JSONObject {
        val json = JSONObject()
            .put("type", "accessibility_event")
            .put("sessionId", sessionId)
            .put("timestampMs", timestampMs)
            .put("elapsedMs", timestampMs - sessionStartMs)
            .put("eventType", event.eventType)
            .put("eventTypeName", AccessibilityEvent.eventTypeToString(event.eventType))

        putOptional(json, "packageName", event.packageName?.toString())
        putOptional(json, "className", event.className?.toString())
        putOptional(json, "contentDescription", event.contentDescription?.toString()?.let(::safeString))
        json.put("text", JSONArray(event.text.mapNotNull { it?.toString()?.let(::safeString) }.take(MAX_LIST_SIZE)))

        val root = service.rootInActiveWindow ?: event.source
        if (root != null) {
            try {
                putOptional(json, "windowPackage", root.packageName?.toString())
                putOptional(json, "windowClass", root.className?.toString())
                val nodes = JSONArray()
                appendNode(root, nodes, 0)
                if (nodes.length() > 0) json.put("nodes", nodes)
            } finally {
                root.recycle()
            }
        }

        return json
    }

    private fun appendNode(node: AccessibilityNodeInfo, out: JSONArray, depth: Int) {
        if (out.length() >= MAX_NODES || depth > MAX_DEPTH) return

        val className = node.className?.toString()
        val item = JSONObject()
            .put("depth", depth)
            .put("clickable", node.isClickable)
            .put("enabled", node.isEnabled)
            .put("focusable", node.isFocusable)

        putOptional(item, "className", className)
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        item.put("bounds", "${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}")

        if (isPasswordLike(node, className)) {
            item.put("redacted", true)
        } else {
            putOptional(item, "text", node.text?.toString()?.let(::safeString))
            putOptional(item, "contentDescription", node.contentDescription?.toString()?.let(::safeString))
        }
        out.put(item)

        for (index in 0 until node.childCount) {
            if (out.length() >= MAX_NODES) break
            val child = node.getChild(index) ?: continue
            try {
                appendNode(child, out, depth + 1)
            } finally {
                child.recycle()
            }
        }
    }

    private fun isPasswordLike(node: AccessibilityNodeInfo, className: String?): Boolean {
        if (node.isPassword) return true
        val lowerClass = className.orEmpty().lowercase(Locale.US)
        val text = node.text?.toString().orEmpty().lowercase(Locale.US)
        val desc = node.contentDescription?.toString().orEmpty().lowercase(Locale.US)
        return lowerClass.contains("password") ||
            text.contains("password") ||
            text.contains("密码") ||
            desc.contains("password") ||
            desc.contains("密码")
    }

    private fun safeString(value: String): String {
        return value
            .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "")
            .take(MAX_STRING_LEN)
    }

    private fun putOptional(json: JSONObject, key: String, value: String?) {
        if (!value.isNullOrBlank()) json.put(key, value)
    }

    private fun writeLineLocked(json: JSONObject) {
        val line = json.toString()
        writer?.write(line)
        writer?.write("\n")
        fileSizeBytes += line.toByteArray(Charsets.UTF_8).size + 1L
    }

    private fun closeWriter() {
        runCatching {
            writer?.flush()
            writer?.close()
        }
        writer = null
    }

    private fun appVersion(service: AccessibilityService): String {
        return runCatching {
            service.packageManager.getPackageInfo(service.packageName, 0).versionName ?: "unknown"
        }.getOrDefault("unknown")
    }
}
