package com.nikgapps.app.data
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class ExplicitRelease(val url: String, val fileName: String) {
    companion object {
        fun parse(value: String): ExplicitRelease {
            val url = value.trim().toHttpUrlOrNull() ?: throw IllegalArgumentException("Enter a valid release download URL")
            require(url.isHttps && url.username.isEmpty() && url.password.isEmpty()) { "Use an HTTPS release URL without credentials" }
            val name = url.pathSegments.lastOrNull { it.endsWith(".zip", true) }
                ?: throw IllegalArgumentException("The release URL must identify a ZIP file")
            require(name.matches(Regex("[A-Za-z0-9][A-Za-z0-9_.-]*\\.zip", RegexOption.IGNORE_CASE))) { "Unsafe release filename" }
            return ExplicitRelease(url.toString(), name)
        }
    }
}
