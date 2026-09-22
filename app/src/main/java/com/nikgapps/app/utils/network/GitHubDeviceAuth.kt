package com.nikgapps.app.utils.network

import android.os.SystemClock
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
    class InvalidTokenException : IOException("GitHub token is invalid or has expired.")

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
        val deadline = SystemClock.elapsedRealtime() + challenge.expiresIn * 1000L
        var interval = challenge.interval
        while (SystemClock.elapsedRealtime() < deadline) {
            delay(interval * 1000L)
            if (SystemClock.elapsedRealtime() >= deadline) break
            val result = try {
                withContext(Dispatchers.IO) {
                    post(
                        "https://github.com/login/oauth/access_token",
                        FormBody.Builder()
                            .add("client_id", clientId)
                            .add("device_code", challenge.deviceCode)
                            .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                            .build(),
                    )
                }
            } catch (failure: GitHubHttpException) {
                if (failure.status == 429) {
                    delay(failure.retryAfterSeconds.coerceAtLeast(60) * 1000L)
                    continue
                }
                if (failure.status !in 500..599) throw failure
                continue
            } catch (_: IOException) {
                // A brief DNS or connection failure must not discard an approved device code.
                continue
            }
            result.optString("access_token").takeIf { it.isNotBlank() }?.let { return it }
            when (result.optString("error")) {
                "authorization_pending" -> Unit
                "slow_down" -> interval = maxOf(interval + 5, result.optInt("interval"))
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
            if (response.code == 401) throw InvalidTokenException()
            if (!response.isSuccessful) throw GitHubHttpException(
                response.code,
                "GitHub account verification failed (${response.code}).",
                response.header("Retry-After")?.toLongOrNull() ?: 0
            )
            val account = JSONObject(response.body?.string().orEmpty())
            AccountProfile(account.getString("login"), account.optString("avatar_url"))
        }
    }

    suspend fun accountProfileWithRetry(token: String): AccountProfile {
        var lastFailure: IOException? = null
        repeat(12) { attempt ->
            try {
                return accountProfile(token)
            } catch (failure: InvalidTokenException) {
                throw failure
            } catch (failure: GitHubHttpException) {
                if (failure.status !in 500..599) throw failure
                lastFailure = failure
                if (attempt < 11) delay(5_000)
            } catch (failure: IOException) {
                lastFailure = failure
                if (attempt < 11) delay(5_000)
            }
        }
        throw lastFailure ?: IOException("Could not reach GitHub to finish sign-in.")
    }

    private class GitHubHttpException(
        val status: Int,
        message: String,
        val retryAfterSeconds: Long = 0
    ) : IOException(message)

    private fun post(url: String, body: FormBody): JSONObject {
        val request = Request.Builder().url(url).post(body)
            .header("Accept", "application/json")
            .header("User-Agent", "NikGapps")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw GitHubHttpException(
                response.code,
                "GitHub sign-in failed (${response.code}).",
                response.header("Retry-After")?.toLongOrNull() ?: 0
            )
            return JSONObject(response.body?.string().orEmpty())
        }
    }
}
