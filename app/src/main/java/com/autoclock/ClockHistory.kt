package com.autoclock

import org.json.JSONArray
import org.json.JSONObject

object ClockHistory {
    private const val DAYS_TO_KEEP = 30L
    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

    fun append(existingJson: String, record: ClockRecord, nowMs: Long = System.currentTimeMillis()): String {
        val cutoffMs = nowMs - DAYS_TO_KEEP * MILLIS_PER_DAY
        val records = (load(existingJson) + record)
            .filter { it.timestampMs >= cutoffMs }
            .sortedByDescending { it.timestampMs }
        return serialize(records)
    }

    fun load(json: String): List<ClockRecord> {
        if (json.isBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val timestampMs = item.optLong("ts", 0L)
                    if (timestampMs <= 0L) continue
                    add(
                        ClockRecord(
                            timestampMs = timestampMs,
                            isClockIn = item.optBoolean("in", true),
                            success = item.optBoolean("ok", false),
                            reason = item.optString("r", "")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun serialize(records: List<ClockRecord>): String {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject()
                    .put("ts", record.timestampMs)
                    .put("in", record.isClockIn)
                    .put("ok", record.success)
                    .put("r", record.reason)
            )
        }
        return array.toString()
    }
}
