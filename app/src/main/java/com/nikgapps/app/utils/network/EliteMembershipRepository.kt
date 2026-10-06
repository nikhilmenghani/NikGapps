package com.nikgapps.app.utils.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.Request

object EliteMembershipRepository {
    private const val MEMBERS_URL =
        "https://raw.githubusercontent.com/nikgapps/tracker/refs/heads/main/folder_access.json"

    suspend fun isElite(username: String): Boolean {
        if (username.isBlank()) return false
        val request = Request.Builder().url(MEMBERS_URL).header("Cache-Control", "no-cache").build()
        NetworkClient.executeRequest(request).use { response ->
            if (!response.isSuccessful) error("Unable to load Elite membership (${response.code})")
            return username.trim().lowercase() in parseUsernames(response.body.string())
        }
    }

    internal fun parseUsernames(text: String): Set<String> =
        Json.parseToJsonElement(text).jsonObject.values.flatMap { value ->
            when (value) {
                is JsonPrimitive -> listOfNotNull(value.takeIf { it.isString }?.content)
                is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.takeIf { entry -> entry.isString }?.content }
                else -> emptyList()
            }
        }.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
}
