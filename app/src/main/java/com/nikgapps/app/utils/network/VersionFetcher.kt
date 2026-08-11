package com.nikgapps.app.utils.network

import com.nikgapps.BuildConfig
import com.nikgapps.app.utils.constants.NetworkConstants.latestVersionUrl
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request

object VersionFetcher {
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
        return try {
            val request = Request.Builder()
                .url(if (BuildConfig.DEBUG) DEV_RELEASES_URL else latestVersionUrl)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .build()
            val response = NetworkClient.executeRequest(request)

            if (response.isSuccessful) {
                if (BuildConfig.DEBUG) parseLatestDevelopmentVersion(response.body.string())
                else parseReleaseVersion(response.body.string())
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Unknown"
        }
    }

    internal fun parseReleaseVersion(responseBody: String): String {
        val jsonElement: JsonElement = Json.decodeFromString(JsonElement.serializer(), responseBody)
        val jsonObject = jsonElement as? JsonObject ?: return "Unknown"
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
            if (!isPrerelease || !tag.startsWith(DEV_TAG_PREFIX)) null
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

    private const val DEV_RELEASES_URL =
        "https://api.github.com/repos/nikhilmenghani/nikgapps/releases?per_page=30"
    private const val DEV_TAG_PREFIX = "dev-v"
}
