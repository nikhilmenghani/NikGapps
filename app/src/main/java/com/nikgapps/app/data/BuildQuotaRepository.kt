package com.nikgapps.app.data

import android.content.Context
import com.nikgapps.BuildConfig
import com.nikgapps.app.utils.network.EliteMembershipRepository

data class BuildQuotaStatus(val successfulBuilds: Int, val limit: Int, val windowMillis: Long,
    val resetsAtMillis: Long?, val eliteResetAvailable: Boolean = false,
    val nextEliteResetAtMillis: Long? = null) {
    val remaining: Int get() = (limit - successfulBuilds).coerceAtLeast(0)
    val allowed: Boolean get() = remaining > 0
}

class BuildQuotaRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun status(now: Long = System.currentTimeMillis()): BuildQuotaStatus = synchronized(LOCK) {
        val baseLimit = preferences.getInt(KEY_LIMIT, if (BuildConfig.DEBUG) TEST_LIMIT else DEFAULT_LIMIT).coerceAtLeast(1)
        val window = preferences.getLong(KEY_WINDOW, DEFAULT_WINDOW_MILLIS).coerceAtLeast(60_000L)
        var startedAt = preferences.getLong(KEY_WINDOW_STARTED_AT, 0L)
        var count = preferences.getInt(KEY_SUCCESS_COUNT, 0)
        val lastEliteResetAt = preferences.getLong(KEY_LAST_ELITE_RESET_AT, 0L)
        if (startedAt != 0L && (now - startedAt >= window || now < startedAt)) {
            startedAt = 0L
            count = 0
            preferences.edit().putLong(KEY_WINDOW_STARTED_AT, startedAt)
                .putInt(KEY_SUCCESS_COUNT, count).remove(KEY_ELITE_WINDOW_LIMIT).commit()
        }
        val limit = if (startedAt > 0L)
            preferences.getInt(KEY_ELITE_WINDOW_LIMIT, 0).takeIf { it > 0 } ?: baseLimit
        else baseLimit
        val resetReady = eliteResetReady(now, lastEliteResetAt, window)
        BuildQuotaStatus(count, limit, window, startedAt.takeIf { it > 0L }?.plus(window),
            eliteResetAvailable = resetReady,
            nextEliteResetAtMillis = if (resetReady) null else lastEliteResetAt + window)
    }

    fun recordSuccess(now: Long = System.currentTimeMillis()): BuildQuotaStatus = synchronized(LOCK) {
        val current = status(now)
        if (!current.allowed) return@synchronized current
        val editor = preferences.edit().putInt(KEY_SUCCESS_COUNT, current.successfulBuilds + 1)
        if (current.resetsAtMillis == null) editor.putLong(KEY_WINDOW_STARTED_AT, now)
        editor.commit()
        status(now)
    }

    suspend fun resetForElite(username: String, expectedRemaining: Int): BuildQuotaStatus {
        require(username.isNotBlank()) { "Sign in with GitHub to reset the build window" }
        check(EliteMembershipRepository.isElite(username)) { "Elite membership could not be verified" }
        return synchronized(LOCK) {
            val now = System.currentTimeMillis()
            val current = status(now)
            check(current.eliteResetAvailable) { "Elite reset is unavailable for this six-hour period" }
            check(current.remaining == expectedRemaining) { "Build count changed; review the reset again" }
            check(preferences.edit()
                .putLong(KEY_WINDOW_STARTED_AT, now)
                .putInt(KEY_SUCCESS_COUNT, 0)
                .putInt(KEY_ELITE_WINDOW_LIMIT, eliteWindowLimit(current.remaining))
                .putLong(KEY_LAST_ELITE_RESET_AT, now)
                .commit()) { "Unable to save Elite reset" }
            status(now)
        }
    }

    companion object {
        const val DEFAULT_LIMIT = 3
        const val TEST_LIMIT = 6
        const val ELITE_BONUS_BUILDS = 6
        const val DEFAULT_WINDOW_MILLIS = 6L * 60L * 60L * 1_000L
        private const val PREFERENCES_NAME = "build_quota"
        private const val KEY_LIMIT = "successful_build_limit"
        private const val KEY_WINDOW = "build_window_millis"
        private const val KEY_WINDOW_STARTED_AT = "window_started_at"
        private const val KEY_SUCCESS_COUNT = "successful_build_count"
        private const val KEY_LAST_ELITE_RESET_AT = "last_elite_reset_at"
        private const val KEY_ELITE_WINDOW_LIMIT = "elite_window_limit"
        private val LOCK = Any()
    }
}

internal fun eliteResetReady(now: Long, lastResetAt: Long, windowMillis: Long): Boolean =
    lastResetAt == 0L || (now >= lastResetAt && now - lastResetAt >= windowMillis)

internal fun eliteWindowLimit(remaining: Int): Int =
    remaining.coerceAtLeast(0) + BuildQuotaRepository.ELITE_BONUS_BUILDS
