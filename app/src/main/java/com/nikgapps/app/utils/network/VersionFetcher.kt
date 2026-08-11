package com.nikgapps.app.utils.network

import com.nikgapps.app.utils.constants.NetworkConstants.latestVersionUrl
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
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
            val request = Request.Builder().url(latestVersionUrl).build()
            val response = NetworkClient.executeRequest(request)

            if (response.isSuccessful) {
                parseReleaseVersion(response.body.string())
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
}
