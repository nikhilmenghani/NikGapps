package com.nikgapps.app.registry

import android.content.Context
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.HttpUrl

internal fun registryRetryAt(retryAfter: String?, reset: String?, now: Long): Long {
    val seconds = retryAfter?.trim()?.toLongOrNull()?.takeIf { it >= 0 }
    val relative = seconds?.takeIf { it <= (Long.MAX_VALUE - now) / 1_000 }?.let { now + it * 1_000 }
    val absolute = retryAfter?.let {
        runCatching { ZonedDateTime.parse(it, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }.getOrNull()
    }
    val resetAt = reset?.toLongOrNull()?.takeIf { it in 0..Long.MAX_VALUE / 1_000 }?.times(1_000)
    return (relative ?: absolute ?: resetAt ?: (now + 60_000)).coerceAtLeast(now + 1_000)
}

fun registryRetryTime(retryAt: Long): String = DateTimeFormatter
    .ofPattern("MMM d, h:mm a z", Locale.getDefault()).withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(retryAt))

class RegistryRateLimitedException(val retryAtMillis: Long) : Exception(
    "NikGapps server is currently down. Retry after ${registryRetryTime(retryAtMillis)}. " +
        "Builds needing uncached packages or build files must wait; cached content is still available."
)

/** One cooldown shared by metadata, packages and builder assets, including after restart. */
object RegistryAvailability {
    private val deadline = MutableStateFlow<Long?>(null)
    val retryAtMillis = deadline.asStateFlow()
    private var save: (Long?) -> Unit = {}

    @Synchronized fun initialize(context: Context) {
        val prefs = context.getSharedPreferences("registry-availability", Context.MODE_PRIVATE)
        save = { value -> prefs.edit().putLong("retry-at", value ?: 0).apply() }
        val stored = prefs.getLong("retry-at", 0).takeIf { it > System.currentTimeMillis() }
        deadline.value = listOfNotNull(stored, deadline.value).maxOrNull()
        clearExpired()
    }

    @Synchronized fun rateLimited(retryAfter: String?, reset: String?, now: Long = System.currentTimeMillis()): Long {
        val value = maxOf(deadline.value ?: 0, registryRetryAt(retryAfter, reset, now))
        deadline.value = value
        save(value)
        return value
    }

    @Synchronized fun clearExpired(now: Long = System.currentTimeMillis()) {
        if ((deadline.value ?: 0) <= now) { deadline.value = null; save(null) }
    }

    fun requireAvailable(url: HttpUrl) {
        if (url.host != "gitlab.com") return
        clearExpired()
        deadline.value?.let { throw RegistryRateLimitedException(it) }
    }
}
