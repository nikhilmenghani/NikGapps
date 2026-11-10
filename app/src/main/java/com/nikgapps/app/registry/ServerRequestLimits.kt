package com.nikgapps.app.registry

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

internal enum class RequestLimitServer(val title: String) { GITLAB("GitLab"), GITHUB("GitHub") }

internal data class ServerRequestLimit(
    val remaining: Long?, val limit: Long?, val resetAtMillis: Long?, val retryAtMillis: Long?,
    val bucket: String?, val httpStatus: Int, val observedAtMillis: Long,
)

/** Header observations, not a prediction: other clients and unreported limits can affect access. */
internal object ServerRequestLimits {
    private val state = MutableStateFlow<Map<RequestLimitServer, ServerRequestLimit>>(emptyMap())
    val observations = state.asStateFlow()
    private val client = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build()

    @Synchronized fun observe(response: Response, now: Long = System.currentTimeMillis()) {
        // Release downloads redirect to storage; quota headers belong to the original API response.
        response.priorResponse?.let { observe(it, now) }
        val url = response.request.url
        if (url.scheme != "https" || url.port != 443) return
        val server = when (url.host) {
            "api.github.com" -> RequestLimitServer.GITHUB
            "gitlab.com" -> RequestLimitServer.GITLAB
            else -> return
        }
        fun number(name: String) = response.header(name)?.toLongOrNull()?.takeIf { it >= 0 }
        val remaining = number("X-RateLimit-Remaining") ?: number("RateLimit-Remaining")
        val limit = number("X-RateLimit-Limit") ?: number("RateLimit-Limit")
        val reset = (number("X-RateLimit-Reset") ?: number("RateLimit-Reset"))
            ?.takeIf { it <= Long.MAX_VALUE / 1_000 }?.times(1_000)
        val retry = response.header("Retry-After")?.let { registryRetryAt(it, null, now) }
        state.value = state.value + (server to ServerRequestLimit(remaining, limit, reset, retry,
            response.header("X-RateLimit-Resource") ?: response.header("RateLimit-Name"), response.code, now))
    }

    /** Explicit developer action: one request, no retries, no credentials sent to GitLab. */
    suspend fun check(server: RequestLimitServer): Unit = withContext(Dispatchers.IO) {
        val request = when (server) {
            RequestLimitServer.GITLAB -> Request.Builder()
                .url("https://gitlab.com/api/v4/projects/85036487/repository/files/catalog.json/raw?ref=main")
                .header("Range", "bytes=0-0").build()
            RequestLimitServer.GITHUB -> RegistryDelivery.prepare(Request.Builder()
                .url("${RegistryDelivery.API}/git/ref/heads/main").build())
        }
        // Headers suffice; close the body without reading the catalog.
        client.newCall(request).execute().use { observe(it) }
    }
}
