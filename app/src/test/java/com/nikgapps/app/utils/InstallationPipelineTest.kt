package com.nikgapps.app.utils
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class InstallationPipelineTest {
    @Test fun failuresStopDependentStages() = runBlocking {
        for (failed in 0..2) {
            val calls = mutableListOf<Int>()
            suspend fun stage(i: Int): Boolean { calls += i; return i != failed }
            try { runInstallation({ stage(0) }, { stage(1) }, { stage(2) }); fail() }
            catch (_: IllegalStateException) { assertEquals((0..failed).toList(), calls) }
        }
        assertTrue(runInstallation({ true }, { true }, { true }))
        var mounted = false
        try { runInstallation({ throw CancellationException() }, { mounted = true; true }, { true }); fail() }
        catch (_: CancellationException) { assertFalse(mounted) }
    }
}
