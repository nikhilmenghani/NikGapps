package com.nikgapps.app.utils.worker

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

/** Persist identity independently of WorkManager retention. KEEP is only the active-project lock. */
object BuildWorkScheduler {
    private val lock = Mutex()
    fun currentOperation(context: Context, projectId: String): String? {
        val prefs = context.getSharedPreferences("build-operations", Context.MODE_PRIVATE)
        val operation = prefs.getString("current:$projectId", null) ?: return null
        val work = prefs.getString("work:$operation", null) ?: return null
        if (prefs.getString("publication:$work", null) != null || prefs.getString("published:$operation", null) != null) return null
        return operation.removePrefix("$projectId-")
    }
    suspend fun enqueue(context: Context, projectId: String, operation: String,
        request: OneTimeWorkRequest, explicitRetry: Boolean): UUID = lock.withLock {
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("build-operations", Context.MODE_PRIVATE)
            if (!explicitRetry) prefs.getString("work:$operation", null)?.let { return@withContext UUID.fromString(it) }
            val manager = WorkManager.getInstance(context)
            val name = BuildZipWorker.uniqueName(projectId)
            val active = manager.getWorkInfosForUniqueWork(name).get().firstOrNull { !it.state.isFinished }
            val id = active?.id ?: request.id
            check(prefs.edit().putString("work:$operation", id.toString()).putBoolean("started:$operation", true)
                .putString("current:$projectId", operation).commit())
            if (active == null) manager.enqueueUniqueWork(name, ExistingWorkPolicy.KEEP, request).result.get()
            id
        }
    }
}
