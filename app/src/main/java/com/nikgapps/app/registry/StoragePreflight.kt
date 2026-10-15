package com.nikgapps.app.registry
import java.io.File

object StoragePreflight {
    fun required(sizes: List<Long?>): Long {
        var total = 32L * 1024 * 1024
        sizes.forEach { size ->
            val bytes = size ?: (128L * 1024 * 1024)
            require(bytes >= 0 && bytes <= (Long.MAX_VALUE - total) / 3) { "Invalid package size" }
            total += bytes * 3
        }
        return total
    }
    fun check(directory: File, sizes: List<Long?>, available: Long =
        generateSequence(directory.absoluteFile) { it.parentFile }.first { it.isDirectory }.usableSpace) {
        val required = required(sizes)
        if (available < required) throw InsufficientStorage(required, available)
    }
}
