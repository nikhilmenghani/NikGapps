package com.nikgapps.app.registry

import com.nikgapps.app.utils.AppDiagnostics
import org.junit.Assert.assertEquals
import org.junit.Test

class AppDiagnosticsTest {
    @Test fun formatsStableSingleLineEvents() {
        assertEquals(
            "area=build action=resolved packages=3 run=abc_123",
            AppDiagnostics.format("build", "resolved", mapOf("run" to "abc 123", "packages" to 3))
        )
    }
}
