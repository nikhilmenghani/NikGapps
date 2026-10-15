package com.nikgapps.app.registry
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.*
import java.nio.file.Files
import com.nikgapps.app.data.shouldEnqueueBuild

class BuildSafetyPolicyTest {
    @Test fun restoredAndCompletedOperationsNeverAutoEnqueue() {
        assertTrue(shouldEnqueueBuild(false, false, false))
        assertFalse(shouldEnqueueBuild(true, false, false)) // Process restored, WorkManager pruned.
        assertFalse(shouldEnqueueBuild(false, true, false)) // Includes finished/awaiting-save work.
        assertFalse(shouldEnqueueBuild(true, true, false)) // Repeated tap or navigation.
        assertTrue(shouldEnqueueBuild(true, true, true)) // Explicit retry only.
    }
    @Test fun preflightHandlesUnknownSizesAndOverflow() {
        assertEquals(32L * 1024 * 1024 + 30, StoragePreflight.required(listOf(10L)))
        assertTrue(StoragePreflight.required(listOf(null)) > StoragePreflight.required(emptyList()))
        assertThrows(IllegalArgumentException::class.java) { StoragePreflight.required(listOf(Long.MAX_VALUE)) }
        assertThrows(InsufficientStorage::class.java) { StoragePreflight.check(java.io.File("."), listOf(10L), available = 0) }
    }
    @Test fun clearingNeverRemovesAnActiveBuildsFiles() = runBlocking {
        val dir = Files.createTempDirectory("cache-lease").toFile()
        try {
            RegistryCache.withLease {
                assertThrows(IllegalStateException::class.java) { RegistryCache.clear(dir) }
            }
            RegistryCache.clear(dir)
            Unit
        } finally { dir.deleteRecursively() }
    }
}
