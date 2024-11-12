package com.nikgapps.app.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

suspend fun fetchLatestVersion(): String {
    return try {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.github.com/repos/nikhilmenghani/nikgapps/releases/latest")
            .build()

        // Execute the request using Dispatchers.IO
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

        if (response.isSuccessful) {
            // Parse the JSON response to get the latest version name
            response.body?.string()?.let { responseBody ->
                val jsonElement: JsonElement = Json.decodeFromString(JsonElement.serializer(), responseBody)
                val jsonObject = jsonElement as? JsonObject
                val versionName = jsonObject?.get("name")?.jsonPrimitive?.content
                versionName.toString().replace("v", "")
            } ?: "Unknown"
        } else {
            "Unknown"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        "Unknown"
    }
}
