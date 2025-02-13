package com.nikgapps.app.utils.network

import com.nikgapps.app.utils.constants.NetworkConstants.latestVersionUrl
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request

object VersionFetcher {
    suspend fun fetchLatestVersion(): String {
        return try {
            val request = Request.Builder().url(latestVersionUrl).build()
            val response = NetworkClient.executeRequest(request)

            if (response.isSuccessful) {
                response.body?.string()?.let { responseBody ->
                    val jsonElement: JsonElement = Json.decodeFromString(JsonElement.serializer(), responseBody)
                    val jsonObject = jsonElement as? JsonObject
                    jsonObject?.get("name")?.jsonPrimitive?.content?.replace("v", "") ?: "Unknown"
                } ?: "Unknown"
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Unknown"
        }
    }
}
