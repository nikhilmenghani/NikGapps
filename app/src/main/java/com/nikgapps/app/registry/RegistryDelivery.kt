package com.nikgapps.app.registry

import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response

internal object RegistryDelivery {
    const val REPOSITORY = "nikgapps/nikgapps-packages"
    const val API = "https://api.github.com/repos/$REPOSITORY"
    var userToken: () -> String = { "" }

    fun trusted(url: HttpUrl): Boolean = url.scheme == "https" && url.host == "api.github.com" && url.port == 443 &&
        url.encodedPath.startsWith("/repos/$REPOSITORY/")

    fun contents(path: String, commit: String? = null): String =
        "$API/contents/$path" + (commit?.let { "?ref=$it" } ?: "")

    fun prepare(request: Request, token: String = userToken()): Request {
        if (request.url.host == "gitlab.com") throw MetadataException(
            "This cached package list uses the previous download service. Refresh package metadata to continue.")
        val builder = request.newBuilder().removeHeader("Authorization")
        if (trusted(request.url)) {
            if (token.isBlank()) throw com.nikgapps.app.utils.network.GitHubBuildAuth.AuthException(
                "Sign in to GitHub to download package data. Saved projects and verified cached files remain available.")
            builder.header("Authorization", "Bearer $token")
            val accept = when {
                request.url.encodedPath.startsWith("/repos/$REPOSITORY/contents/") -> "application/vnd.github.raw+json"
                request.url.encodedPath.startsWith("/repos/$REPOSITORY/releases/assets/") -> "application/octet-stream"
                else -> "application/vnd.github+json"
            }
            builder.header("Accept", accept).header("User-Agent", "NikGapps")
                .header("X-GitHub-Api-Version", "2022-11-28")
        }
        return builder.build()
    }

    fun rateLimited(response: Response): Boolean = trusted(response.request.url) &&
        (response.code == 429 || (response.code == 403 &&
            (response.header("X-RateLimit-Remaining") == "0" || response.header("Retry-After") != null)))
}
