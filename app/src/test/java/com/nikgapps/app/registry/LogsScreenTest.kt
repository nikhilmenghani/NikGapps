package com.nikgapps.app.registry

import com.nikgapps.app.presentation.ui.screen.appendLogBatch
import com.nikgapps.app.presentation.ui.screen.isViewerRenderNoise
import com.nikgapps.app.presentation.ui.screen.parseLogcatLine
import org.junit.Assert.*
import org.junit.Test

class LogsScreenTest {
    @Test fun recognizesViewerRenderNoiseWithoutDroppingOrdinaryAppLogs() {
        val frame = parseLogcatLine("08-10 12:00:00.001  123  123 V View: setRequestedFrameRate frameRate=NaN")
        val vri = parseLogcatLine("08-10 12:00:00.002  123  123 V VRI[MainActivity]: Requested frameRateCategory 6")
        val app = parseLogcatLine("08-10 12:00:00.003  123  123 I Registry: Package validated")
        assertTrue(isViewerRenderNoise(frame))
        assertTrue(isViewerRenderNoise(vri))
        assertFalse(isViewerRenderNoise(app))
    }

    @Test fun batchAppendPublishesOneSnapshot() {
        val initial = listOf(parseLogcatLine("first"))
        val batch = listOf(parseLogcatLine("second"), parseLogcatLine("third"))
        assertEquals(listOf("first", "second", "third"), appendLogBatch(initial, batch).map { it.raw })
    }
}
