package com.pdfsuite.app.pdf

import java.util.Locale

/** Renders a byte count as a short human-readable size (e.g. "2.3 MB"). */
fun Long.toReadableSize(): String {
    if (this < 1024) return "$this B"
    val units = arrayOf("KB", "MB", "GB")
    var value = this / 1024.0
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[unitIndex])
}
