package com.pdfsuite.app.pdf

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Unwraps a Compose [Context] (often a ContextWrapper) down to its underlying Activity, if any. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
