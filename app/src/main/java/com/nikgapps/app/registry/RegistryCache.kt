package com.nikgapps.app.registry

import java.io.File

data class CacheClearResult(val files: Int, val bytes: Long)

object RegistryCache {
    private val relativeDirectories = listOf(
        "nikgapps/metadata",
        "nikgapps/packages",
        "nikgapps/builder-assets",
        "zip-builds"
    )

    fun clear(cacheDirectory: File): CacheClearResult {
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
        return CacheClearResult(files, bytes)
    }
}
