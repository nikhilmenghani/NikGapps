package com.nikgapps.app.analytics

/** GitHub identity verified at build start; never contains credentials. */
data class BuildAnalyticsIdentity(val githubUsername: String) {
    init { require(githubUsername.isNotBlank()) }
    fun properties(): Map<String, Any> = mapOf("account_mode" to "signed_in", "github_username" to githubUsername)
}

internal fun buildEventProperties(current: Map<String, Any>, identity: BuildAnalyticsIdentity?): Map<String, Any> =
    if (identity == null) current else (current - "github_username" - "account_mode") + identity.properties()
