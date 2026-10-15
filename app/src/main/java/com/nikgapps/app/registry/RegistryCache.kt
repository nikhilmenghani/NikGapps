package com.nikgapps.app.registry

import java.io.File

data class CacheClearResult(val files: Int, val bytes: Long)

object RegistryCache {
    private val lock = Any()
    private var active = 0
    fun <T> withReadLease(block: () -> T): T {
        synchronized(lock) { active++ }
        try { return block() } finally { synchronized(lock) { active-- } }
    }
    suspend fun <T> withLease(block: suspend () -> T): T {
        synchronized(lock) { active++ }
        try { return block() } finally { synchronized(lock) { active-- } }
    }
    private val relativeDirectories = listOf(
        "nikgapps/metadata",
        "nikgapps/packages",
        "nikgapps/builder-assets",
        "zip-builds"
    )

    fun clear(cacheDirectory: File): CacheClearResult = synchronized(lock) {
        check(active == 0) { "A build or download is active. Try clearing the cache after it finishes." }
        var files = 0
        var bytes = 0L
        relativeDirectories.map { File(cacheDirectory, it) }.forEach { directory ->
            if (directory.isDirectory) {
                directory.walkTopDown().filter(File::isFile).forEach {
                    files++
                    bytes += it.length()
                }
            }
            check(!directory.exists() || directory.deleteRecursively()) {
                "Unable to clear cached build data in ${directory.name}"
            }
        }
        CacheClearResult(files, bytes)
    }
}
