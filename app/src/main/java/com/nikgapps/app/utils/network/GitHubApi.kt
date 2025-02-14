package com.nikgapps.app.utils.network

import android.os.Build
import androidx.annotation.RequiresApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Base64
import android.util.Base64 as AndroidBase64

object GitHubApi {
    private const val BASE_URL = "https://api.github.com/repos"

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun createOrUpdateFile(
        token: String,
        owner: String = "nikgapps",
        repo: String = "tracker",
        path: String = "data/records.json",
        commitMessage: String,
        newFileContent: String,
        branch: String = "main"
    ) {
        val url = "$BASE_URL/$owner/$repo/contents/$path"
        val sha = getFileSha(token, url)

        val encodedContent = Base64.getEncoder().encodeToString(newFileContent.toByteArray(Charsets.UTF_8))
        val payloadJson = JSONObject().apply {
            put("message", commitMessage)
            put("content", encodedContent)
            put("branch", branch)
            sha?.let { put("sha", it) } // Include SHA for updates
        }

        val requestBody = payloadJson.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "token $token")
            .addHeader("Accept", "application/vnd.github+json")
            .put(requestBody)
            .build()

        NetworkClient.executeRequestAsync(request, object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (response.isSuccessful) {
                    println("File created/updated successfully: ${response.body?.string()}")
                } else {
                    println("Error creating/updating file: ${response.body?.string()}")
                }
            }
        })
    }

    private suspend fun getFileSha(token: String, url: String): String? {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "token $token")
            .addHeader("Accept", "application/vnd.github+json")
            .build()

        return try {
            val response = NetworkClient.executeRequest(request)
            if (response.isSuccessful) {
                val body = response.body?.string()
                JSONObject(body ?: "").optString("sha", null)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun fetchJsonFile(
        token: String,
        filePath: String,
        onResult: (String) -> Unit
    ) {
        val url = "$BASE_URL/nikgapps/tracker/contents/$filePath"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "token $token")
            .addHeader("Accept", "application/vnd.github+json")
            .build()

        NetworkClient.executeRequestAsync(request, object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onResult("Error: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (response.code == 404) {
                    onResult("File doesn't exist")
                } else if (response.isSuccessful) {
                    response.body?.string()?.let { bodyStr ->
                        try {
                            val jsonResponse = JSONObject(bodyStr)
                            val encodedContent = jsonResponse.getString("content").replace("\n", "")
                            val decodedBytes = AndroidBase64.decode(encodedContent, AndroidBase64.DEFAULT)
                            val decodedContent = String(decodedBytes, Charsets.UTF_8)
                            onResult(decodedContent)
                        } catch (e: Exception) {
                            onResult("Error parsing JSON: ${e.message}")
                        }
                    } ?: onResult("Empty response")
                } else {
                    onResult("Error: ${response.message}")
                }
            }
        })
    }
}
