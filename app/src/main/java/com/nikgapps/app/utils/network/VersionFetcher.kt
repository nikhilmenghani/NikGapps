package com.nikgapps.app.utils.network

import com.nikgapps.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import kotlinx.coroutines.CancellationException
import java.util.concurrent.ConcurrentHashMap

object VersionFetcher {
    const val SOURCE_REPOSITORY = "nikhilmenghani/NikGapps"
    const val RELEASE_REPOSITORY = "nikhilmenghani/NikGappsReleases"
    private val downloadUrls = ConcurrentHashMap<String, String>()
    internal data class AppRelease(val version: String, val url: String)

    fun downloadUrl(version: String): String = downloadUrls[version] ?: artifactUrl(SOURCE_REPOSITORY, version, BuildConfig.DEBUG)

    internal fun artifactUrl(repository: String, version: String, development: Boolean): String {
        val tag = if (development) "dev-v$version" else "v$version"
        val file = if (development) "NikGapps-debug-$tag.apk" else "NikGapps-$tag.apk"
        return "https://github.com/$repository/releases/download/$tag/$file"
    }

    internal suspend fun resolveRelease(installed: String, development: Boolean,
        fetch: suspend (String) -> String?): AppRelease? {
        suspend fun from(repository: String): AppRelease? = try {
            val body = fetch(repository)
            if (body == null) null else {
                val version = if (development) parseLatestDevelopmentVersion(body) else parseReleaseVersion(body)
                if (!Regex("[0-9]+(?:\\.[0-9]+){1,2}").matches(version)) null
                else AppRelease(version, artifactUrl(repository, version, development))
            }
        } catch (cancelled: CancellationException) { throw cancelled }
          catch (_: Exception) { null }
        val primary = from(SOURCE_REPOSITORY)
        if (primary != null && isNewer(primary.version, installed)) return primary
        val fallback = from(RELEASE_REPOSITORY)
        return if (fallback != null && (primary == null || isNewer(fallback.version, primary.version))) fallback else primary
    }
    fun isNewer(candidate: String, installed: String): Boolean {
        val left = candidate.trimStart('v').split('.', '-', '_').map { it.toIntOrNull() ?: 0 }
        val right = installed.trimStart('v').split('.', '-', '_').map { it.toIntOrNull() ?: 0 }
        repeat(maxOf(left.size, right.size)) { index ->
            val comparison = left.getOrElse(index) { 0 }.compareTo(right.getOrElse(index) { 0 })
            if (comparison != 0) return comparison > 0
        }
        return false
    }

    suspend fun fetchLatestVersion(): String {
        val release = resolveRelease(BuildConfig.VERSION_NAME, BuildConfig.DEBUG) { repository ->
            val request = Request.Builder()
                .url("https://api.github.com/repos/$repository/releases" + if (BuildConfig.DEBUG) "?per_page=100" else "/latest")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .build()
            NetworkClient.executeRequest(request).use { response ->
                if (response.isSuccessful) response.body.string() else null
            }
        }
        if (release != null) downloadUrls[release.version] = release.url
        return release?.version ?: "Unknown"
    }

    internal fun parseReleaseVersion(responseBody: String): String {
        val jsonElement: JsonElement = Json.decodeFromString(JsonElement.serializer(), responseBody)
        val jsonObject = jsonElement as? JsonObject ?: return "Unknown"
        if (jsonObject["draft"]?.jsonPrimitive?.content == "true" ||
            jsonObject["prerelease"]?.jsonPrimitive?.content == "true") return "Unknown"
        val version = jsonObject["tag_name"]?.jsonPrimitive?.content
            ?: jsonObject["name"]?.jsonPrimitive?.content
            ?: return "Unknown"
        return version.trim().removePrefix("v").ifEmpty { "Unknown" }
    }

    internal fun parseLatestDevelopmentVersion(responseBody: String): String {
        val releases = Json.decodeFromString(JsonElement.serializer(), responseBody).jsonArray
        return releases.mapNotNull { element ->
            val release = element.jsonObject
            val isPrerelease = release["prerelease"]?.jsonPrimitive?.content == "true"
            val tag = release["tag_name"]?.jsonPrimitive?.content.orEmpty()
            if (release["draft"]?.jsonPrimitive?.content == "true" || !isPrerelease || !tag.startsWith(DEV_TAG_PREFIX)) null
            else tag.removePrefix(DEV_TAG_PREFIX).takeIf { isValidVersion(it) }
        }.maxWithOrNull(::compareVersions) ?: "Unknown"
    }

    private fun isValidVersion(version: String): Boolean =
        version.split('.').size == 3 && version.split('.').all { it.toIntOrNull() != null }

    private fun compareVersions(left: String, right: String): Int {
        val leftParts = left.split('.').map(String::toInt)
        val rightParts = right.split('.').map(String::toInt)
        repeat(maxOf(leftParts.size, rightParts.size)) { index ->
            val comparison = leftParts.getOrElse(index) { 0 }
                .compareTo(rightParts.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }

    private const val DEV_TAG_PREFIX = "dev-v"
}
