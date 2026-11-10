package com.nikgapps.app.registry

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

internal fun <T> OkHttpClient.executeRegistryRequest(
    request: Request,
    maxAttempts: Int = 5,
    read: (Response) -> T,
): T {
    val prepared = RegistryDelivery.prepare(request)
    var lastFailure: Throwable? = null
    repeat(maxAttempts) { attempt ->
        try {
            RegistryAvailability.requireAvailable(prepared.url)
            newCall(prepared).execute().use { response ->
                if (response.isSuccessful) return read(response)
                if (RegistryDelivery.rateLimited(response)) {
                    throw RegistryRateLimitedException(RegistryAvailability.rateLimited(
                        response.header("Retry-After"), response.header("X-RateLimit-Reset")))
                }
                val retryable = response.code == 429 || response.code in 500..599
                if (!retryable || attempt == maxAttempts - 1)
                    error("HTTP ${response.code} for ${request.url}")
                val retryAfter = response.header("Retry-After")?.toLongOrNull()?.let {
                    TimeUnit.SECONDS.toMillis(it)
                }
                Thread.sleep(retryAfter ?: (1_000L shl attempt).coerceAtMost(30_000L))
            }
        } catch (failure: Throwable) {
            if (failure is RegistryRateLimitedException) throw failure
            if (failure is InterruptedException) {
                Thread.currentThread().interrupt()
                throw failure
            }
            lastFailure = failure
            if (attempt == maxAttempts - 1) throw failure
            Thread.sleep((1_000L shl attempt).coerceAtMost(30_000L))
        }
    }
    throw IllegalStateException("Registry request failed", lastFailure)
}
