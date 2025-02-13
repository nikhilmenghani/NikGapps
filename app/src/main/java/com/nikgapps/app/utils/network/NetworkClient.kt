package com.nikgapps.app.utils.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

object NetworkClient {
    private val client = OkHttpClient()

    suspend fun executeRequest(request: Request): Response {
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute()
        }
    }

    fun executeRequestAsync(request: Request, callback: okhttp3.Callback) {
        client.newCall(request).enqueue(callback)
    }
}
