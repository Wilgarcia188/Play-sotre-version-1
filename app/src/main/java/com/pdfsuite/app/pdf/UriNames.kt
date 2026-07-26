package com.pdfsuite.app.pdf

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/** Best-effort display name for a content [Uri], falling back to the last path segment. */
fun Uri.displayName(context: Context): String {
    val cursor = context.contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) return it.getString(index) ?: fallbackName()
        }
    }
    return fallbackName()
}

/** Best-effort file size in bytes for a content [Uri], or null if unavailable. */
fun Uri.fileSizeBytes(context: Context): Long? {
    val cursor = context.contentResolver.query(this, arrayOf(OpenableColumns.SIZE), null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && !it.isNull(index)) return it.getLong(index)
        }
    }
    return null
}

private fun Uri.fallbackName(): String = lastPathSegment ?: toString()
