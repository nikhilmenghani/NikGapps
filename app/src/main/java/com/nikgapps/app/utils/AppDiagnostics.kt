package com.nikgapps.app.utils

import android.util.Log

object AppDiagnostics {
    const val TAG = "NikGappsFlow"

    fun info(area: String, action: String, details: Map<String, Any?> = emptyMap()) {
        runCatching { Log.i(TAG, format(area, action, details)) }
    }

    fun failure(area: String, action: String, error: Throwable, details: Map<String, Any?> = emptyMap()) {
        runCatching {
            Log.e(TAG, format(area, action, details + ("error" to (error.message ?: error.javaClass.simpleName))), error)
        }
    }

    internal fun format(area: String, action: String, details: Map<String, Any?>): String = buildString {
        append("area=").append(clean(area)).append(" action=").append(clean(action))
        details.toSortedMap().forEach { (key, value) ->
            if (value != null) append(' ').append(clean(key)).append('=').append(clean(value.toString()))
        }
    }

    private fun clean(value: String): String = value.replace(Regex("[\\r\\n\\t ]+"), "_").take(300)
}
