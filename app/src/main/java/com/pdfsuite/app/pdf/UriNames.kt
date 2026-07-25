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

private fun Uri.fallbackName(): String = lastPathSegment ?: toString()
