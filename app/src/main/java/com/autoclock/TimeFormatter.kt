package com.autoclock

import java.util.Locale

fun formatTimeLabel(hour: Int, minute: Int): String {
    return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
}
