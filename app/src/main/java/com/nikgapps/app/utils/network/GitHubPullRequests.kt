package com.nikgapps.app.utils.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class GitHubPullRequest(
    val number: Int,
    val title: String,
    val url: String,
    val state: String,
    val createdAt: String,
    val updatedAt: String,
    val isDraft: Boolean,
    val isMerged: Boolean,
)

data class GitHubPullRequestSearch(
    val totalCount: Int,
    val incomplete: Boolean,
    val pullRequests: List<GitHubPullRequest>,
)

object GitHubPullRequests {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun createdBy(token: String, username: String): GitHubPullRequestSearch =
        withContext(Dispatchers.IO) {
            val url = "https://api.github.com/search/issues".toHttpUrl().newBuilder()
                .addQueryParameter("q", "repo:nikgapps/config is:pr author:$username")
                .addQueryParameter("sort", "created")
                .addQueryParameter("order", "desc")
                .addQueryParameter("per_page", "100")
                .build()
            val request = Request.Builder().url(url)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "NikGapps")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 401) throw GitHubDeviceAuth.InvalidTokenException()
                if (!response.isSuccessful) {
                    val apiMessage = runCatching {
                        JSONObject(response.body.string()).optString("message")
                    }.getOrNull().orEmpty()
                    throw IOException(
                        apiMessage.ifBlank { "Could not load GitHub pull requests (${response.code})." }
                    )
                }
                parse(JSONObject(response.body.string()))
            }
        }

    internal fun parse(root: JSONObject): GitHubPullRequestSearch {
        val items = root.optJSONArray("items")
        val pullRequests = buildList {
            if (items != null) for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                val details = item.optJSONObject("pull_request")
                add(
                    GitHubPullRequest(
                        number = item.getInt("number"),
                        title = item.getString("title"),
                        url = item.getString("html_url"),
                        state = item.optString("state", "closed"),
                        createdAt = item.optString("created_at"),
                        updatedAt = item.optString("updated_at"),
                        isDraft = item.optBoolean("draft"),
                        isMerged = details?.has("merged_at") == true && !details.isNull("merged_at"),
                    )
                )
            }
        }
        return GitHubPullRequestSearch(
            totalCount = root.optInt("total_count", pullRequests.size),
            incomplete = root.optBoolean("incomplete_results"),
            pullRequests = pullRequests,
        )
    }
}
