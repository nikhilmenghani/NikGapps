package com.nikgapps.app.utils.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** GitHub OAuth device flow; the client ID is public, while tokens remain on the device. */
object GitHubDeviceAuth {
    data class AccountProfile(val login: String, val avatarUrl: String)

    data class Challenge(
        val deviceCode: String,
        val userCode: String,
        val verificationUri: String,
        val expiresIn: Int,
        val interval: Int,
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun start(clientId: String): Challenge = withContext(Dispatchers.IO) {
        val response = post(
            "https://github.com/login/device/code",
            FormBody.Builder().add("client_id", clientId).add("scope", "repo").build(),
        )
        Challenge(
            response.getString("device_code"),
            response.getString("user_code"),
            response.getString("verification_uri"),
            response.getInt("expires_in"),
            response.optInt("interval", 5).coerceAtLeast(5),
        )
    }

    suspend fun awaitToken(clientId: String, challenge: Challenge): String {
        val deadline = System.currentTimeMillis() + challenge.expiresIn * 1000L
        var interval = challenge.interval
        while (System.currentTimeMillis() < deadline) {
            delay(interval * 1000L)
            val result = withContext(Dispatchers.IO) {
                post(
                    "https://github.com/login/oauth/access_token",
                    FormBody.Builder()
                        .add("client_id", clientId)
                        .add("device_code", challenge.deviceCode)
                        .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                        .build(),
                )
            }
            result.optString("access_token").takeIf { it.isNotBlank() }?.let { return it }
            when (result.optString("error")) {
                "authorization_pending" -> Unit
                "slow_down" -> interval += 5
                "expired_token" -> throw IOException("GitHub sign-in code expired. Try again.")
                "access_denied" -> throw IOException("GitHub sign-in was denied.")
                else -> throw IOException(result.optString("error_description", "GitHub sign-in failed."))
            }
        }
        throw IOException("GitHub sign-in code expired. Try again.")
    }

    suspend fun accountProfile(token: String): AccountProfile = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("https://api.github.com/user")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "NikGapps")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("GitHub account verification failed (${response.code}).")
            val account = JSONObject(response.body?.string().orEmpty())
            AccountProfile(account.getString("login"), account.optString("avatar_url"))
        }
    }

    private fun post(url: String, body: FormBody): JSONObject {
        val request = Request.Builder().url(url).post(body)
            .header("Accept", "application/json")
            .header("User-Agent", "NikGapps")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("GitHub sign-in failed (${response.code}).")
            return JSONObject(response.body?.string().orEmpty())
        }
    }
}
