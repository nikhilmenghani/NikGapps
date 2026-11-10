package com.nikgapps.app.registry

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.*
import org.junit.Test

class ServerRequestLimitsTest {
    private fun response(url: String, vararg headers: Pair<String, String>, code: Int = 200) = Response.Builder()
        .request(Request.Builder().url(url).build()).protocol(Protocol.HTTP_1_1).code(code).message("fixture")
        .apply { headers.forEach { header(it.first, it.second) } }.build()

    @Test fun readsTheActualGitLabHeadersWithoutTreatingTheCountAsGithubQuota() {
        ServerRequestLimits.observe(response("https://gitlab.com/api/v4/projects/85036487",
            "ratelimit-limit" to "500", "ratelimit-remaining" to "499",
            "ratelimit-reset" to "1794317940", "ratelimit-name" to "throttle_unauthenticated_api"), 1234)
        val value = ServerRequestLimits.observations.value.getValue(RequestLimitServer.GITLAB)
        assertEquals(499L, value.remaining)
        assertEquals(500L, value.limit)
        assertEquals(1794317940000L, value.resetAtMillis)
        assertEquals("throttle_unauthenticated_api", value.bucket)
        assertEquals(1234L, value.observedAtMillis)
    }

    @Test fun preservesGithubQuotaAcrossAssetRedirectsAndIgnoresStorageHeaders() {
        val api = response("${RegistryDelivery.API}/releases/assets/123", "X-RateLimit-Limit" to "5000",
            "X-RateLimit-Remaining" to "4980", "X-RateLimit-Resource" to "core", code = 302)
        val storage = response("https://release-assets.githubusercontent.com/file", "X-RateLimit-Remaining" to "1")
            .newBuilder().priorResponse(api).build()
        ServerRequestLimits.observe(storage, 1000)
        assertEquals(4980L, ServerRequestLimits.observations.value.getValue(RequestLimitServer.GITHUB).remaining)
    }

    @Test fun missingHeadersReplaceOldCountsWithUnknownAndRetryTimeStillShows() {
        ServerRequestLimits.observe(response("https://gitlab.com/api/v4/projects/85036487",
            "RateLimit-Remaining" to "10"), 1000)
        ServerRequestLimits.observe(response("https://gitlab.com/api/v4/projects/85036487",
            "Retry-After" to "3600", "RateLimit-Limit" to "-1", "RateLimit-Reset" to Long.MAX_VALUE.toString(), code = 429), 2000)
        val value = ServerRequestLimits.observations.value.getValue(RequestLimitServer.GITLAB)
        assertNull(value.remaining)
        assertNull(value.limit)
        assertNull(value.resetAtMillis)
        assertEquals(3602000L, value.retryAtMillis)
        assertEquals(429, value.httpStatus)
    }
}
