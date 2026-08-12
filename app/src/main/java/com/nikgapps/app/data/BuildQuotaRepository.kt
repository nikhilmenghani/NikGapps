package com.nikgapps.app.data

import android.content.Context
import com.nikgapps.BuildConfig

data class BuildQuotaStatus(val successfulBuilds: Int, val limit: Int, val windowMillis: Long,
    val resetsAtMillis: Long?) {
    val remaining: Int get() = (limit - successfulBuilds).coerceAtLeast(0)
    val allowed: Boolean get() = remaining > 0
}

class BuildQuotaRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun status(now: Long = System.currentTimeMillis()): BuildQuotaStatus = synchronized(LOCK) {
        val limit = preferences.getInt(KEY_LIMIT, if (BuildConfig.DEBUG) TEST_LIMIT else DEFAULT_LIMIT).coerceAtLeast(1)
        val window = preferences.getLong(KEY_WINDOW, DEFAULT_WINDOW_MILLIS).coerceAtLeast(60_000L)
        var startedAt = preferences.getLong(KEY_WINDOW_STARTED_AT, 0L)
        var count = preferences.getInt(KEY_SUCCESS_COUNT, 0)
        if (startedAt != 0L && (now - startedAt >= window || now < startedAt)) {
            startedAt = 0L
            count = 0
            preferences.edit().putLong(KEY_WINDOW_STARTED_AT, startedAt).putInt(KEY_SUCCESS_COUNT, count).commit()
        }
        BuildQuotaStatus(count, limit, window, startedAt.takeIf { it > 0L }?.plus(window))
    }

    fun recordSuccess(now: Long = System.currentTimeMillis()): BuildQuotaStatus = synchronized(LOCK) {
        val current = status(now)
        if (!current.allowed) return@synchronized current
        val editor = preferences.edit().putInt(KEY_SUCCESS_COUNT, current.successfulBuilds + 1)
        if (current.successfulBuilds == 0) editor.putLong(KEY_WINDOW_STARTED_AT, now)
        editor.commit()
        status(now)
    }

    companion object {
        const val DEFAULT_LIMIT = 3
        const val TEST_LIMIT = 6
        const val DEFAULT_WINDOW_MILLIS = 6L * 60L * 60L * 1_000L
        private const val PREFERENCES_NAME = "build_quota"
        private const val KEY_LIMIT = "successful_build_limit"
        private const val KEY_WINDOW = "build_window_millis"
        private const val KEY_WINDOW_STARTED_AT = "window_started_at"
        private const val KEY_SUCCESS_COUNT = "successful_build_count"
        private val LOCK = Any()
    }
}
