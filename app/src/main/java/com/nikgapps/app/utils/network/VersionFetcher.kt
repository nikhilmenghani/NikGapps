package com.nikgapps.app.utils.network

import com.nikgapps.BuildConfig
import com.nikgapps.app.data.GithubPrefs
import com.nikgapps.app.data.UpdatePrefs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

object VersionFetcher {
    const val RELEASE_REPOSITORY = "nikhilmenghani/NikGappsReleases"
    private val downloadUrls = ConcurrentHashMap<String, String>()
    private val releaseMutex = Mutex()
    internal data class AppRelease(val version: String, val url: String)

    fun downloadUrl(version: String): String = downloadUrls[version] ?: artifactUrl(RELEASE_REPOSITORY, version, BuildConfig.DEBUG)

    internal fun artifactUrl(repository: String, version: String, development: Boolean): String {
        val tag = if (development) "dev-v$version" else "v$version"
        val file = if (development) "NikGapps-debug-$tag.apk" else "NikGapps-$tag.apk"
        return "https://github.com/$repository/releases/download/$tag/$file"
    }

    internal suspend fun resolveRelease(development: Boolean,
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
        return from(RELEASE_REPOSITORY)
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

    internal fun releaseRequest(development: Boolean, token: String): Request = Request.Builder()
        .url("https://api.github.com/repos/$RELEASE_REPOSITORY/releases" + if (development) "?per_page=100" else "/latest")
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .header("User-Agent", "NikGapps")
        .apply { if (token.isNotBlank()) header("Authorization", "Bearer ${token.trim()}") }
        .build()

    /** Only validated successful lookups replace the persisted, channel-specific version. */
    internal suspend fun fetchRelease(
        development: Boolean,
        token: String,
        readCachedVersion: () -> String?,
        saveVersion: (String) -> Unit,
        execute: suspend (Request) -> String?
    ): AppRelease? {
        val fresh = resolveRelease(development) { execute(releaseRequest(development, token)) }
        if (fresh != null) {
            saveVersion(fresh.version)
            return fresh
        }
        // Read after the failed lookup so a success from another caller is retained.
        return readCachedVersion()?.takeIf { validCachedVersion(it, development) }?.let {
            AppRelease(it, artifactUrl(RELEASE_REPOSITORY, it, development))
        }
    }

    private fun validCachedVersion(version: String, development: Boolean): Boolean =
        Regex("[0-9]+(?:\\.[0-9]+){1,2}").matches(version) && (!development || isValidVersion(version))

    /** Available before a network request finishes, including after restarting the app. */
    fun lastKnownVersion(): String? = cachedVersion(BuildConfig.DEBUG)

    private fun cachedVersion(development: Boolean): String? =
        (if (development) UpdatePrefs.lastSuccessfulDevelopmentRelease else UpdatePrefs.lastSuccessfulStableRelease)
            .takeIf { validCachedVersion(it, development) }

    suspend fun fetchLatestVersion(): String = releaseMutex.withLock {
        val development = BuildConfig.DEBUG
        val release = fetchRelease(development, GithubPrefs.token,
            readCachedVersion = { cachedVersion(development) },
            saveVersion = { version ->
                if (development) UpdatePrefs.lastSuccessfulDevelopmentRelease = version
                else UpdatePrefs.lastSuccessfulStableRelease = version
            }) { request ->
            NetworkClient.executeRequest(request).use { response ->
                if (response.isSuccessful) response.body.string() else null
            }
        }
        if (release != null) downloadUrls[release.version] = release.url
        release?.version ?: "Unknown"
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
