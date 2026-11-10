package com.nikgapps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import com.nikgapps.app.network.RegistryProjectContent
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.nikgapps.app.network.InternetRequiredGate
import com.nikgapps.app.registry.RegistryAvailability
import com.nikgapps.app.registry.RegistryRateLimitedException
import com.nikgapps.app.registry.RegistryDelivery
import com.nikgapps.app.registry.executeRegistryRequest
import com.nikgapps.app.registry.registryRetryTime
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Uses only the disposable review app; no personal projects or network settings are changed. */
class RegistryRateLimitInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun serverCooldownReplacesProjectsKeepsHeaderAndRestoresCachedNavigation() {
        check(InstrumentationRegistry.getInstrumentation().targetContext.packageName == "com.nikgapps.review.debug")
        RegistryAvailability.clearExpired(Long.MAX_VALUE)
        val previousToken = RegistryDelivery.userToken
        RegistryDelivery.userToken = { "fixture-user-token" }
        var calls = 0
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            calls++
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(429).message("Too Many Requests").header("Retry-After", "600")
                .body("rate limited".toResponseBody()).build()
        }.build()
        try {
            compose.setContent {
                MaterialTheme {
                    InternetRequiredGate(required = true, allowOffline = false, onOpenAppSettings = {}) {
                        var opened by remember { mutableStateOf(false) }
                        val retryAt by RegistryAvailability.retryAtMillis.collectAsState()
                        Column(Modifier.fillMaxSize()) {
                            Text("Fixed header")
                            RegistryProjectContent(retryAt, Modifier.weight(1f)) {
                                Column {
                                    Text(if (opened) "Cached project apps" else "Saved projects")
                                    Button(onClick = { opened = true }) { Text("Open cached project") }
                                }
                            }
                        }
                    }
                }
            }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("You're offline").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithText("Open cached project").fetchSemanticsNodes().isNotEmpty() }
            val headerBefore = compose.onNodeWithText("Fixed header").fetchSemanticsNode().boundsInRoot
            val request = Request.Builder().url("https://api.github.com/repos/nikgapps/nikgapps-packages/contents/fixture.json").build()
            val error = assertThrows(RegistryRateLimitedException::class.java) {
                client.executeRegistryRequest(request) { it.body.string() }
            }
            compose.onNodeWithText("NikGapps server is currently down").assertIsDisplayed()
            compose.onNodeWithText("Please try again after ${registryRetryTime(error.retryAtMillis)}.").assertIsDisplayed()
            compose.onNodeWithText("You're offline").assertDoesNotExist()
            compose.onNodeWithText("Open cached project").assertDoesNotExist()
            assertEquals(headerBefore, compose.onNodeWithText("Fixed header").fetchSemanticsNode().boundsInRoot)
            assertEquals(1, calls)
            RegistryAvailability.clearExpired(Long.MAX_VALUE)
            compose.onNodeWithTag("registry-rate-limit").assertDoesNotExist()
            compose.onNodeWithText("Open cached project").performClick()
            compose.onNodeWithText("Cached project apps").assertIsDisplayed()
        } finally {
            RegistryDelivery.userToken = previousToken
            RegistryAvailability.clearExpired(Long.MAX_VALUE)
        }
    }
}
